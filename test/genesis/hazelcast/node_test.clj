(ns genesis.hazelcast.node-test
  (:require [genesis.hazelcast.node :as node]))

(defn getting-started
  [node]
  (let [m (doto (.getMap node "customers")
            (.put 1 "Joe")
            (.put 1 "Ali")
            (.put 1 "Avi"))
        q (doto (.getQueue node "customers")
            (.offer "Tom")
            (.offer "Mary")
            (.offer "Jane"))]
    
    (println "Customer with key 1: " (.get m 1))
    (println "Map Size: " (.size m))

    (println "First customer: " (.poll q))
    (println "Second customer: " (.peek q))
    (println "Queue size: " (.size q))))
