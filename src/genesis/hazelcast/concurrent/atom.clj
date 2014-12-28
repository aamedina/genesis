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

(ns genesis.hazelcast.concurrent.atom
  (:import [com.hazelcast.core IAtomicReference HazelcastInstance]
           [clojure.lang IFn IPersistentMap]))

(definterface IValidate
  (^void validate [val])
  (^void validate [^clojure.lang.IFn f val]))

(deftype DistributedAtom [^IAtomicReference state
                          ^:volatile-mutable ^IFn validator
                          ^:volatile-mutable ^IPersistentMap watches
                          ^:unsynchronized-mutable ^IPersistentMap meta]
  clojure.lang.IAtom
  (swap [_ f])
  (swap [_ f x])
  (swap [_ f x y])
  (swap [_ f x y args])
  (compareAndSet [_ oldval newval])
  (reset [_ newval])

  clojure.lang.IRef
  (deref [_] (.get state))
  (setValidator [_ f])
  (getValidator [_])
  (getWatches [_])
  (addWatch [_ k f])
  (removeWatch [_ k])

  clojure.lang.IReference
  (meta [_] (locking meta meta))
  (alterMeta [_ f args]
    (locking meta
      (set! meta (apply f meta args))))
  (resetMeta [_ m]
    (locking meta
      (set! meta m)))

  IValidate
  (validate [this val]
    (.validate this validator val))
  (validate [_ f val]
    (try
      (when (or (nil? f) (false? (boolean (f val))))
        (throw (IllegalStateException. "Invalid reference state")))
      (catch RuntimeException e
        (throw e))
      (catch Exception e
        (throw (IllegalStateException. "Invalid reference state" e))))))

(defn make-distributed-atom
  [node x & {:keys [meta]}]
  (let [ref (.getAtomicReference node (name (gensym "atom")))]
    (.set ref x)
    (DistributedAtom. ref nil {} meta)))
