;; Copyright 2015 Adrian Medina.
;; Genesis is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program. If not, see <http://www.gnu.org/licenses/>.

(ns genesis.hazelcast.node
  (:require [genesis.protocols :as p]
            [com.stuartsierra.component :as c])
  (:import com.hazelcast.core.Hazelcast
           com.hazelcast.core.HazelcastInstance
           java.util.Queue))

(defonce ^:dynamic *hazelcast* nil)

(defrecord HazelcastNode [instance delayed]
  p/Node
  
  c/Lifecycle
  (start [this]
    (if instance
      this
      (try
        (binding [*hazelcast* (Hazelcast/newHazelcastInstance)]
          @delayed
          (assoc this
            :instance *hazelcast*))
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))))
  (stop [this]
    (if instance
      (try
        (.shutdown instance)
        (assoc this :instance nil)
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))
      this)))

(defmacro node
  [& body]
  `(HazelcastNode. nil (delay ~@body)))

(defmacro defnode
  [node-name & body]
  `(def ~node-name (node ~@body)))

(defn make-node
  []
  (node (let [m (doto (.getMap *hazelcast* "customers")
                  (.put 1 "Joe")
                  (.put 1 "Ali")
                  (.put 1 "Avi"))
              q (doto (.getQueue *hazelcast* "customers")
                  (.offer "Tom")
                  (.offer "Mary")
                  (.offer "Jane"))]
          
          (println "Customer with key 1: " (.get m 1))
          (println "Map Size: " (.size m))

          (println "First customer: " (.poll q))
          (println "Second customer: " (.peek q))
          (println "Queue size: " (.size q)))))
