(ns genesis.hazelcast.cluster-test
  (:require [genesis.protocols :as p]
            [genesis.hazelcast.cluster :as clst]
            [genesis.hazelcast.node :as n]
            [genesis.hazelcast.client :as cl]
            [genesis.hazelcast.node-test :as n-test]
            [genesis.hazelcast.client-test :as cl-test]
            [com.stuartsierra.component :as c]))

(defonce cluster nil)

(defn setup-test-cluster
  []
  (alter-var-root #'cluster (fn [_]
                              (clst/system {:f n-test/getting-started
                                            :num-nodes 2}))))

(defn getting-started
  []
  (setup-test-cluster)
  (let [cluster (alter-var-root #'cluster c/start-system)
        client (c/start (cl/make-client cl-test/getting-started))]
    (c/stop client)
    cluster))
