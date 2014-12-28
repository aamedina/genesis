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

(ns genesis.services.store
  (:require [genesis.hazelcast.node :as node]
            [genesis.protocols :as p]
            [genesis.database :as db])
  (:import [com.hazelcast.core MapStore]))

(deftype ClusterAwareStore [node]
  MapStore
  (delete [_ k])
  (deleteAll [_ ks])
  (store [_ k v])
  (storeAll [_ m])
  (load [_ k])
  (loadAll [_ ks])
  (loadAllKeys [_]))

(defn make-cluster-aware-store
  [node]
  (ClusterAwareStore. node))
