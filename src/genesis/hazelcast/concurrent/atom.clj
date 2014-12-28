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

(definterface IWatchable
  (^void notifyWatches [oldval newval]))

(deftype DistributedAtom [^IAtomicReference state
                          ^:volatile-mutable ^IFn validator
                          ^:volatile-mutable ^IPersistentMap watches
                          ^:unsynchronized-mutable ^IPersistentMap meta]
  clojure.lang.IAtom
  (swap [this f]
    (let [oldval (.get state)
          newval (f oldval)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur f))))
  (swap [this f x]
    (let [oldval (.get state)
          newval (f oldval x)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur f x))))
  (swap [this f x y]
    (let [oldval (.get state)
          newval (f oldval x y)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur f x y))))
  (swap [this f x y args]
    (let [oldval (.get state)
          newval (apply f oldval x y args)]
      (if (.compareAndSet this oldval newval)
        newval
        (recur f x y args))))
  (compareAndSet [this oldval newval]
    (.validate this newval)
    (let [ret (.compareAndSet state oldval newval)]
      (when ret
        (.notifyWatches this oldval newval))
      ret))
  (reset [this newval]
    (let [oldval (.get state)]
      (.validate this newval)
      (.set state newval)
      (.notifyWatches this oldval newval)
      newval))

  clojure.lang.IRef
  (deref [_] (.get state))
  (setValidator [this f]
    (.validate this f (.deref this))
    (set! validator f))
  (getValidator [_] validator)
  (getWatches [_] watches)
  (addWatch [this k f]
    (locking watches
      (set! watches (.assoc watches k f))
      this))
  (removeWatch [this k]
    (locking watches
      (set! watches (.without watches k))
      this))

  IWatchable
  (notifyWatches [this oldval newval]
    (doseq [[k f] watches]
      (f k this oldval newval)))

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
  [node x & {:keys [meta validator]}]
  (let [ref (.getAtomicReference node (name (gensym "atom")))]
    (.set ref x)
    (DistributedAtom. ref validator clojure.lang.PersistentHashMap/EMPTY meta)))
