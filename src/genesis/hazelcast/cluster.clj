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

(ns genesis.hazelcast.cluster
  (:require [genesis.protocols :as p]
            [genesis.jetty :refer [make-webapp]]
            [genesis.hazelcast.node :as node]
            [com.stuartsierra.component :as c]
            [environ.core :refer [env]])
  (:import com.hazelcast.core.Hazelcast
           com.hazelcast.core.HazelcastInstance))

(defn make-mancenter
  []
  (make-webapp (env :mancenter-host)
               (env :mancenter-port)
               (env :mancenter-war)))

(defrecord Cluster [nodes mancenter num-nodes f]
  p/Cluster

  c/Lifecycle
  (start [this]
    (if nodes
      this
      (try
        (let [mancenter (make-mancenter)
              nodes (into #{} (map c/start)
                          (repeatedly num-nodes #(node/make-node f)))]
          (assoc this
            :nodes nodes
            :mancenter mancenter))
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))))
  (stop [this]
    (if nodes
      (try
        (c/stop mancenter)
        (doseq [node nodes]
          (c/stop node))
        (assoc this
          :nodes nil
          :mancenter nil)
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))
      this)))

(defn make-cluster
  ([f] (make-cluster 1 f))
  ([num-nodes f]
   (Cluster. nil nil num-nodes f)))
