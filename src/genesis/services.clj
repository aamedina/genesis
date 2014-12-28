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

(ns genesis.services
  (:require [genesis.protocols :as p]
            [genesis.services.store :as store]
            [genesis.services.cache :as cache]
            [genesis.services.load-balancer :as load]))

(defn service?
  [x]
  (satisfies? p/Service x))

(defn service
  [node properties terminate-on-shutdown?
   & {store 0 cache 1 load-balancer 2
      :or {store (store/make-cluster-aware-store node)
           cache (cache/make-cluster-aware-cache node)
           load-balancer (load/make-cluster-aware-load-balancer node)}}]
  (reify
    p/Service
    (store [this] store)
    (cache [this] cache)
    (load-balancer [this] load-balancer)
    
    p/ManagedService
    (node [_] node)
    (properties [_] properties)
    (terminate-on-shutdown? [_] terminate-on-shutdown?)

    com.hazelcast.spi.ManagedService))

(defmacro defservice
  [service-name & impls]
  `(defn ~service-name
     [node# properties# terminate-on-shutdown?#]
     (let [properties# (doto (java.util.Properties.)
                         (.putAll properties#))
           s# (service node# properties# terminate-on-shutdown?# ~@impls)
           cls# (.getName (class s#))
           config# (com.hazelcast.config.Config.)
           services-config# (doto (.getServicesConfig config#)
                              (.setEnableDefaults true))
           service-config# (doto (.getServiceConfig services-config#)
                             (.setEnabled true)
                             (.setName ~service-name)
                             (.setClassName class-name#))]
       (doseq [[k v] properties#]
         (.setProperty service-config# k v))
       s#)))
