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

(ns genesis.distributed.atom
  (:require [genesis.distributed.services :refer [defservice]]
            [taoensso.nippy :as nippy :refer [freeze thaw]])
  (:import [com.hazelcast.core HazelcastInstance DistributedObject]
           [com.hazelcast.spi NodeEngine ManagedService RemoteService]
           [com.hazelcast.spi AbstractOperation PartitionAwareOperation]
           [com.hazelcast.partition.strategy StringPartitioningStrategy]))

(defonce partitioning-strategy
  StringPartitioningStrategy/INSTANCE)

(defmacro command
  [& sigs]
  (let [[interfaces [sigs]] (if (symbol? (first sigs))
                              (split-with symbol? sigs)
                              [[] [sigs]])]
    `(let [fn# (fn ~@sigs)]
       (proxy [AbstractOperation
               ~@interfaces
               clojure.lang.IObj
               clojure.lang.Fn
               clojure.lang.IFn
               java.util.Comparator
               java.io.Serializable
               java.lang.Runnable
               java.util.concurrent.Callable] []
         (returnsResponse [] true)
         (getResponse [] (.run ~(symbol "this")))
         (writeInternal [out#] (.writeObject out# fn#))
         (readInternal [in#] (.readObject in#))
         (call [] (.call fn#))
         (run [] (.run fn#))
         (invoke ~@(if (vector? (first sigs)) [sigs] sigs))
         (compare [o1# o2#] (.compare fn# o1# o2#))
         (meta [] (.meta fn#))
         (withMeta [meta#] (.withMeta fn# meta#))))))

(defservice Counter
  :service [^:unsynchronized-mutable ^NodeEngine node-engine]
  ManagedService
  (init [_ engine properties]
    (println "CounterService.init")
    (set! node-engine engine))
  (shutdown [_ terminate?]
    (println "CounterService.shutdown"))
  (reset [_])

  RemoteService
  (createDistributedObject [this id]
    (Counter. id node-engine this))
  (destroyDistributedObject [_ id])

  :proxy [id ^NodeEngine node-engine ^RemoteService service]
  DistributedObject
  (destroy [this]
    (let [proxy-service (.getProxyService node-engine)]
      (.destroyDistributedObject (.getServiceName this) (.getName this))))
  (getName [_] id)
  (getPartitionKey [_] (.getPartitionKey partitioning-strategy id))
  (getServiceName [_] "CounterService")
  
  :operation
  ICounter
  (inc [amount] (inc amount)))
