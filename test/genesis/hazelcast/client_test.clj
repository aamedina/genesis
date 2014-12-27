(ns genesis.hazelcast.client-test
  (:require [genesis.hazelcast.client :as client]))

(defn getting-started
  [client config]
  (let [m (.getMap client "customers")]
    (println "Map Size: " (.size m))))
