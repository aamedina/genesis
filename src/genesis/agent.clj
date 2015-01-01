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

(ns genesis.agent
  (:require [genesis.core :refer :all])
  (:import [genesis.atom IValidate IWatchable]
           [clojure.lang IFn ISeq IPersistentMap PersistentHashMap]
           [java.util.concurrent Executor]
           [com.hazelcast.core HazelcastInstance IAtomicReference IMap]
           [com.hazelcast.core IAtomicLong IExecutorService]))

(definterface IAgent
  (dispatch [^clojure.lang.IFn f
             ^clojure.lang.ISeq args
             ^java.util.concurrent.Executor exec]))

(deftype Agent [^IAtomicReference state
                ^IAtomicReference validator
                ^IMap watches
                ^IAtomicLong send-off-thread-pool-counter
                ^IAtomicReference meta]
  IAgent
  (dispatch [_ f args exec])
  
  clojure.lang.IRef
  (deref [_] (.get state))
  (setValidator [this f]
    (.validate this f (.get state))
    (.set validator f))
  (getValidator [_] (.get validator))
  (getWatches [_] watches)
  (addWatch [this k f] (.put watches k f))
  (removeWatch [this k] (.remove watches k))

  IWatchable
  (notifyWatches [this oldval newval]
    (doseq [[k f] watches]
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

(defn make-agent
  ([agent-name] (make-agent agent-name nil))
  ([agent-name state & options]
   (let [node (find-node)
         exec (.getExecutorService node "exec")]
     )))
