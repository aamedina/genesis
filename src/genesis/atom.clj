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

(ns genesis.atom
  (:require [genesis.core :refer :all])
  (:import [clojure.lang IFn IPersistentMap PersistentHashMap]
           [com.hazelcast.core HazelcastInstance IAtomicReference]))

(definterface IValidate
  (^void validate [val])
  (^void validate [^clojure.lang.IFn f val]))

(definterface IWatchable
  (^void notifyWatches [oldval newval]))

(deftype Atom [^IAtomicReference state
               ^IAtomicReference validator
               ^IAtomicReference watches
               ^IAtomicReference meta]
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
    (.validate this f (.get state))
    (.set validator f))
  (getValidator [_] (.get validator))
  (getWatches [_] (.get watches))
  (addWatch [this k f] (.set watches (.assoc (.get watches) k f)))
  (removeWatch [this k] (.set watches (.without (.get watches) k)))

  IWatchable
  (notifyWatches [this oldval newval]
    (doseq [[k f] (.get watches)]
      (f k this oldval newval)))

  clojure.lang.IReference
  (meta [_] (.get meta))
  (alterMeta [_ f args]
    (.set meta (apply f (.get meta) args)))
  (resetMeta [_ m]
    (.set meta m))

  IValidate
  (validate [this val]
    (.validate this (.get validator) val))
  (validate [_ f val]
    (try
      (when (and (not (nil? f)) (false? (boolean (f val))))
        (throw (IllegalStateException. "Invalid reference state")))
      (catch RuntimeException e
        (throw e))
      (catch Exception e
        (throw (IllegalStateException. "Invalid reference state" e))))))

(defn make-atom
  [ref-name x & {:keys [validator meta] :or {meta {}}}]
  (let [ref-name (name ref-name)
        node (find-node)
        ref (.getAtomicReference node ref-name)
        validator-ref (.getAtomicReference node (str ref-name "vf"))
        watcher-ref (.getAtomicReference node (str ref-name "watcher"))
        meta-ref (.getAtomicReference node (str ref-name "meta"))]

    (when (.isNull ref)
      (.set ref x))

    (when (.isNull validator-ref)
      (.set validator-ref validator))
    
    (when (.isNull watcher-ref)
      (.set watcher-ref PersistentHashMap/EMPTY))

    (when (.isNull meta-ref)
      (.set meta-ref meta))
    
    (Atom. ref validator-ref watcher-ref meta-ref)))

(defn find-atom
  [ref-name]
  (make-atom ref-name nil))
