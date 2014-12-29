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

(ns genesis.distributed.services
  (:require [genesis.distributed.configuration :refer [configure]])
  (:import [com.hazelcast.core HazelcastInstance DistributedObject]
           [com.hazelcast.config Config]
           [com.hazelcast.spi NodeEngine ManagedService RemoteService]))

(defmethod configure ::service
  [cls & properties]
  (let [config (Config.)
        services-config (doto (.getServicesConfig config)
                          (.setEnableDefaults true))
        service-config (doto (.getServiceConfig services-config)
                         (.setEnabled true)
                         (.setName (.getName cls))
                         (.setClassName (.getName cls)))]
    (doseq [[k v] properties]
      (.setProperty service-config k v))))

(defmacro defservice
  [name {:keys [service proxy operation]} & opts+specs]
  (let [[opts specs] (loop [opts {}
                            [k v & more :as specs] opts+specs]
                       (if (keyword? k)
                         (recur (assoc opts k v) more)
                         [opts (into {} (comp (partition-all 2)
                                              (map (fn [[[sym] [& impls]]]
                                                     [(resolve sym) impls])))
                                     (partition-by symbol? specs))]))
        _ (println specs)
        managed-service-specs (get specs ManagedService)
        remote-service-specs (get specs RemoteService)
        proxy-specs (flatten (seq (select-keys specs [DistributedObject])))
        specs (flatten (seq (dissoc specs ManagedService RemoteService)))
        service-name (symbol (str name "Service"))
        proxy-name (symbol (str name "Proxy"))
        [op & sigs] operation
        op-interface (gensym op)
        this (symbol "this")
        engine (second proxy)]
    `(do (definterface ~op-interface ~@(filter vector? sigs))
         (deftype ~proxy-name ~proxy
           ~@proxy-specs)
         (deftype ~service-name ~service
           ManagedService
           ~@managed-service-specs
           RemoteService
           ~@remote-service-specs
           ~op-interface
           (~op ~@(for [[arglist & body :as sig] sigs]
                    (list (vec (cons this arglist))
                          `(let [op# (genesis.distributed.atom/command ~@sig)
                                 pid# (.. ~engine
                                          (getPartitionService)
                                          (getPartitionId (.getName ~this)))
                                 builder# (.. ~engine
                                              (getOperationService)
                                              (createInvocationBuilder
                                               (str ~service-name) op# pid#))]
                             @(.invoke builder#))))))
         (derive ~service-name :genesis.distributed.services/service)
         (configure (class ~service-name) opts)
         (class ~name))))
