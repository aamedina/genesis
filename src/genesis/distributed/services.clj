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
  (:require [genesis.distributed.configuration :refer [configure]]
            [clojure.string :as str])
  (:import [com.hazelcast.core HazelcastInstance DistributedObject]
           [com.hazelcast.config Config ServiceConfig ServicesConfig]
           [com.hazelcast.spi NodeEngine ManagedService RemoteService]))

(defmethod configure ::service
  [cls & {:keys [config properties]}]
  (let [service-name (peek (str/split (.getName cls) #"\."))
        service-config (doto (ServiceConfig.)
                         (.setEnabled true)
                         (.setName service-name)
                         (.setClassName (.getName cls)))
        services-config (doto (.getServicesConfig config)
                          (.setEnableDefaults true)
                          (.addServiceConfig service-config))]
    (println service-name (.getName cls))
    (doseq [[k v] properties]
      (.setProperty service-config k v))
    service-config))

(defmacro defservice
  [name & opts+specs]
  (let [m (into {} (comp (partition-all 2)
                         (map (fn [[k v]] [(first k) v])))
                (partition-by keyword? opts+specs))
        [op-interface [op arglist & body]] (get m :operation)
        service-name (symbol (str name "Service"))
        this (symbol "this")
        engine (second (first (get m :proxy)))]
    `(do (definterface ~op-interface (~op ~arglist))
         (deftype ~name ~@(get m :proxy))
         (deftype ~service-name ~@(get m :service)
           ~op-interface
           (~op ~(vec (cons this arglist))
             (let [op# (genesis.distributed.atom/command ~arglist
                                                         ~@body)
                   pid# (.. ~engine
                            (getPartitionService)
                            (getPartitionId (.getName ~this)))
                   builder# (.. ~engine
                                (getOperationService)
                                (createInvocationBuilder
                                 (str ~service-name) op# pid#))]
               @(.invoke builder#))))
         (derive ~service-name :genesis.distributed.services/service)
         ~name)))
