(ns genesis.hazelcast.cluster-test
  (:require [genesis.protocols :as p]
            [genesis.hazelcast.cluster :as clst]
            [genesis.hazelcast.node :as n]
            [genesis.hazelcast.client :as cl]
            [genesis.hazelcast.node-test :as n-test]
            [genesis.hazelcast.client-test :as cl-test]
            [com.stuartsierra.component :as c]))

(defn getting-started
  []
  (let [cluster (c/start (clst/make-cluster 2 n-test/getting-started))
        client (c/start (cl/make-client cl-test/getting-started))]
    (c/stop client)
    (c/stop cluster)))
