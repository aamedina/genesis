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

(ns genesis.cache
  (:require [genesis.core :refer :all]
            [clojure.core.cache :as cache :refer [defcache]])
  (:import [javax.cache Cache Caching CacheManager]
           [javax.cache.configuration MutableConfiguration]))

(defcache JCache [^Cache cache ^String cache-name]
  Object
  (toString [_] (str (into {} (map #(vector (.getKey %) (.getValue %))) cache)))
  
  cache/CacheProtocol
  (lookup [_ k]
    (.get cache k))
  (lookup [_ k not-found]
    (or (.get cache k) not-found))
  (has? [_ k]
    (.containsKey cache k))
  (hit [this k] this)
  (miss [this k v]
    (.put cache k v)
    this)
  (evict [this k]
    (.remove cache k)
    this)
  (seed [this base]
    (let [provider (Caching/getCachingProvider)
          manager (.getCacheManager provider)
          config (MutableConfiguration.)
          cache (or (.getCache manager cache-name)
                    (.createCache manager cache-name config))]
      (doseq [cache-entry base
              :let [[k v] [(.getKey cache-entry) (.getValue cache-entry)]]]
        (.put cache k v))
      (JCache. cache cache-name))))

(defn make-jcache
  ([cache-name] (make-jcache cache-name nil))
  ([cache-name base & options]
   (.seed (JCache. nil cache-name) base)))

(defmethod print-method JCache
  [m writer]
  (.write writer (.toString m)))

(prefer-method print-method JCache clojure.lang.IPersistentMap)
