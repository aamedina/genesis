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
  (:require [genesis.core :refer :all]
            [genesis.protocols :as p]
            [genesis.jetty :refer [make-webapp]]
            [genesis.hazelcast.node :as node]
            [genesis.hazelcast.services :as svc]
            [com.stuartsierra.component :as c]
            [environ.core :refer [env]])
  (:import [com.hazelcast.core Hazelcast HazelcastInstance]
           [com.hazelcast.config XmlConfigBuilder]))

(defn make-mancenter
  []
  (make-webapp {:host (env :mancenter-host)
                :port (env :mancenter-port)
                :context-path "/mancenter"
                :war (env :mancenter-war)}))

(defrecord Cluster [nodes mancenter num-nodes f]
  p/Cluster

  c/Lifecycle
  (start [this]
    (if nodes
      this
      (try
        (let [config (.build (XmlConfigBuilder.))
              _ (doseq [service (svc/declared-services)]
                  (svc/register-service config service))
              nodes (into #{} (map c/start)
                          (repeatedly num-nodes #(node/make-node f config)))]
          (assoc this
            :nodes nodes))
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))))
  (stop [this]
    (if nodes
      (try
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
  [num-nodes f]
  (Cluster. nil nil num-nodes f))

(defn system
  [{:keys [f num-nodes] :or {num-nodes 0}}]
  (c/system-map
   :mancenter (make-mancenter)
   :cluster (c/using (make-cluster num-nodes f)
              [:mancenter])))
