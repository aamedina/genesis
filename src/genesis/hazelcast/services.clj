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

(ns genesis.hazelcast.services
  (:require [genesis.core :refer :all]
            [genesis.protocols :as p]
            [com.stuartsierra.component :as c]
            [clojure.string :as str])
  (:import [com.hazelcast.core HazelcastInstance]
           [com.hazelcast.config Config ServiceConfig ServicesConfig]
           [com.hazelcast.spi ManagedService NodeEngine]
           [java.util Properties]))

(definterface Counter
  (^int inc [^int amount]))

(deftype CounterService [^:unsynchronized-mutable __node-engine]
  ManagedService
  (init [_ node-engine properties]
   (println "CounterService.init")
   (set! __node-engine node-engine))
  (shutdown [_ terminate?]
    (println "CounterService.shutdown"))
  (reset [_]))

(defn derive-service
  [class]
  (let [class-name (.getName class)]
    (when-let [cls (->> (descendants ::services)
                      (filter #(= (.getName %) class-name))
                      first)]
      (underive cls ::services))
    (derive class ::services)))

(defn register-service
  [config service & properties]
  (let [service-class-name (.getName service)
        service-name (peek (str/split service-class-name #"\."))
        services-config (.getServicesConfig config)
        service-config (doto (ServiceConfig.)
                         (.setName service-name)
                         (.setClassName service-class-name)
                         (.setEnabled true))]
    (doseq [[k v] properties]
      (.setProperty service-config k v))
    (.addServiceConfig services-config service-config)
    config))

(derive-service CounterService)

(defn declared-services
  []
  (descendants ::services))
