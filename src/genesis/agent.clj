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
  (:refer-clojure :exclude [send shutdown-agents])
  (:require [genesis.core :refer :all])
  (:import [genesis.atom IValidate IWatchable]
           [clojure.lang IFn ISeq IPersistentMap PersistentHashMap]
           [java.util.concurrent Executor]
           [com.hazelcast.core HazelcastInstance IAtomicReference IMap IQueue]
           [com.hazelcast.core IAtomicLong IExecutorService]))

(definterface IAgent
  (dispatch [^clojure.lang.IFn f
             ^clojure.lang.ISeq args])
  (dispatch [^clojure.lang.IFn f
             ^clojure.lang.ISeq args
             ^java.util.concurrent.Executor exec])
  (doRun [^clojure.lang.IFn f ^clojure.lang.ISeq args])
  (setState [newval]))

(declare make-action)

(deftype Agent [^IAtomicReference state
                ^IExecutorService exec
                ^IAtomicLong send-off-thread-pool-counter
                ^IQueue action-queue
                ^IAtomicReference error
                ^IAtomicReference error-mode
                ^IAtomicReference error-handler
                ^IAtomicReference validator
                ^IMap watches                
                ^IAtomicReference meta
                ^String agent-name]
  IAgent
  (dispatch [this f args] (.dispatch this f args exec))
  (dispatch [_ f args exec]
    (.set state (apply f (.get state) args)))
  (doRun [_ f args]
    (let [nested (ThreadLocal.)]
      (try
        (let [oldval (.get state)
              newval (f oldval args)]
          (.setState agent newval)
          (.notifyWatches agent oldval newval))
        (catch Throwable e
          (.set (.-error agent) e)))
      ))
  (setState [this newval]
    (.validate this newval)
    (let [ret (not= (.get state) newval)]
      (.set state newval)
      ret))
  
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
   (let [agent-name (name agent-name)
         node (find-node)
         exec (.getExecutorService node "exec")
         state (.getAtomicReference node (str agent-name "-agent-state"))
         counter (.getAtomicLong node (str agent-name "-counter"))
         action-queue (.getQueue node (str agent-name "-action-queue"))
         error (.getAtomicReference node (str agent-name "-agent-error"))
         error-mode (.getAtomicReference node (str agent-name "-error-mode"))
         handler (.getAtomicReference node (str agent-name "-error-handler"))
         validator (.getAtomicReference node (str agent-name "-agent-vf"))
         watches (.getMap node (str agent-name "-agent-watches"))
         meta (.getAtomicReference node (str agent-name "-agent-meta"))]
     (Agent. state exec counter action-queue error error-mode handler
             validator watches meta agent-name))))

(defn send
  [a f & args]
  (.dispatch a f args))

(defn shutdown-agents
  []
  (when-let [node (find-node)]
    (.shutdown (.getExecutorService node "exec"))))

(defn make-action
  [agent-name f args]
  (reify Runnable
    (run [_]
      (.doRun (make-agent agent-name) f args))
    java.io.Serializable))
