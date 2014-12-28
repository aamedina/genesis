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

(ns genesis.services.cache
  (:require [genesis.protocols :as p]
            [clojure.core.cache :as cache :refer [defcache]]))

(defn cache?
  [x]
  (satisfies? cache/CacheProtocol x))

(defcache ClusterAwareCache [cache node]
  p/ClusterAware
  
  cache/CacheProtocol
  (lookup [_ item]
    (get cache item))
  (lookup [_ item not-found]
    (get cache item not-found))
  (has? [_ item]
    (contains? cache item))
  (hit [this item] this)
  (miss [_ item result]
    (ClusterAwareCache. (assoc cache item result) node))
  (evict [_ key]
    (ClusterAwareCache. (dissoc cache key) node))
  (seed [_ base]
    (ClusterAwareCache. base node)))

(defn make-cluster-aware-cache
  [node]
  (ClusterAwareCache. {} node))
