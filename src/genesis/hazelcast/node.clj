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
  (:require [genesis.core :refer :all]
            [genesis.vars :as vars]
            [genesis.protocols :as p]
            [com.stuartsierra.component :as c])
  (:import com.hazelcast.core.Hazelcast
           com.hazelcast.core.HazelcastInstance
           java.util.Queue))

(defrecord Node [node f]
  p/Node
  
  c/Lifecycle
  (start [this]
    (if node
      this
      (try
        (let [node (Hazelcast/newHazelcastInstance)]
          (setup-vars node)
          (f node)
          (assoc this
            :node node))
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))))
  (stop [this]
    (if node
      (try
        (.shutdown node)
        (assoc this :node nil)
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))
      this)))

(defn make-node
  [f]
  (Node. nil f))
