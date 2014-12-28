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
  (let [cluster (c/start-system (clst/system {:f n-test/getting-started
                                              :num-nodes 2}))
        client (c/start (cl/make-client cl-test/getting-started))]
    (c/stop client)
    (c/stop-system cluster)))
