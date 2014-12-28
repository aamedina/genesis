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
  (:require [genesis.services.store :as store]
            [genesis.services.cache :as cache]
            [genesis.services.load-balancer :as load]))

(defprotocol Service
  (store [this])
  (cache [this])
  (load-balancer [this]))

(defn service?
  [x]
  (satisfies? Service x))

(defn service
  ([] (service nil))
  ([store] (service store nil))
  ([store cache] (service store cache nil))
  ([store cache load-balancer]
   (reify Service
     (store [this] store)
     (cache [this] cache)
     (load-balancer [this] load-balancer))))

(defmacro defservice
  [service-name & [store cache load-balancer]]
  `(defonce ~service-name (service ~store ~cache ~load-balancer)))

(defn extend-service
  [service & {store 0 cache 1 load-balancer 2
              :or {store (genesis.services/store service)
                   cache (genesis.services/cache service)
                   load-balancer (genesis.services/load-balancer service)}}]
  (service store cache load-balancer))
