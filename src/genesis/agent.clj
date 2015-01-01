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
  (:refer-clojure :exclude [send agent-error shutdown-agents restart-agent
                            await-for])
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
  (setErrorMode [^clojure.lang.Keyword k])
  (^clojure.lang.Keyword getErrorMode [])
  (setErrorHandler [^clojure.lang.IFn f])
  (^clojure.lang.IFn getErrorHandler [])
  (restart [new-state clear-action?])
  (releasePendingSends [])
  (setState [newval])
  (execute [^Runnable action])
  (enqueue [^Runnable action]))

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
                ^String agent-name
                ^ThreadLocal nested]
  IAgent
  (dispatch [this f args] (.dispatch this f args exec))
  (dispatch [this f args exec]
    (when-let [err (.get error)]
      (throw (ex-info "Agent is failed, needs restart" {:error err})))
    (let [action (make-action agent-name f args)]
      (if-let [actions (.get nested)]
        (.set nested (conj actions action))
        (.enqueue this action)))
    this)
  (doRun [this f args]
    (try
      (.set nested [])
      (try
        (let [oldval (.get state)
              newval (apply f oldval args)]
          (.setState this newval)
          (.notifyWatches this oldval newval))
        (catch Throwable e
          (.set error e)))
      (if (.get error)
        (do (.set nested nil)
            (try
              (when-let [f (.get error-handler)]
                (f this (.get error)))
              (catch Throwable e))
            (when (identical? (.get error-mode) :continue)
              (.set error nil)))
        (.releasePendingSends this))
      (when-let [next-action (and (nil? (.get error)) (.peek action-queue))]
        (.execute this next-action))
      (finally
        (.set nested nil))))
  (setState [this newval]
    (.validate this newval)
    (let [ret (not= (.get state) newval)]
      (when ret
        (.set state newval))
      ret))
  (setErrorMode [_ k] (.set error-mode k))
  (getErrorMode [_] error-mode)
  (setErrorHandler [_ f] (.set error-handler f))
  (getErrorHandler [_] error-handler)
  (restart [this new-state clear-actions?]
    (when (nil? (.get error))
      (throw (ex-info "Agent does not need a restart" {:agent this})))
    (.validate this new-state)
    (.set state new-state)
    (if clear-actions?
      (doseq [action action-queue]
        (.remove action-queue action))
      (when-let [prior-action (.peek action-queue)]
        (.execute this prior-action))))
  (releasePendingSends [this]
    (if-let [sends (.get nested)]
      (do (doseq [action sends]
            (.enqueue this action))
          (.set nested [])
          (count sends))
      0))
  (execute [this action]
    (try
      (.execute exec action)
      (catch Throwable e
        (if-let [f (.get error-handler)]
          (try
            (f this e)
            (catch Throwable e))))))
  (enqueue [this action]
    (let [prior-action (.peek action-queue)]
      (if (and (nil? prior-action) (nil? (.get error)))
        (.execute this action)
        (.offer action-queue action))))
  
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
  ([agent-name state & {:keys [validator error-mode meta error-handler]}]
   (let [agent-name (name agent-name)
         node (find-node)
         exec (.getExecutorService node "genesis-agent-send-pool")
         agent-state (.getAtomicReference node (str agent-name "-agent-state"))
         counter (.getAtomicLong node (str agent-name "-counter"))
         action-queue (.getQueue node (str agent-name "-action-queue"))
         error (.getAtomicReference node (str agent-name "-agent-error"))
         err-mode (.getAtomicReference node (str agent-name "-error-mode"))
         handler (.getAtomicReference node (str agent-name "-error-handler"))
         validator-ref (.getAtomicReference node (str agent-name "-agent-vf"))
         watches (.getMap node (str agent-name "-agent-watches"))
         meta-ref (.getAtomicReference node (str agent-name "-agent-meta"))]
     
     (when (and (.isNull agent-state) state)
       (.set agent-state state))

     (when (.isNull err-mode)
       (.set err-mode (or error-mode :continue)))

     (when (and (.isNull handler) error-handler)
       (.set handler error-handler))    

     (when (.isNull validator-ref)
       (.set validator-ref validator))
     
     (when (.isNull meta-ref)
       (.set meta-ref (or meta {})))
     
     (Agent. agent-state exec counter action-queue error error-mode handler
             validator-ref watches meta-ref agent-name (ThreadLocal.)))))

(defn send
  [a f & args]
  (.dispatch a f args))

(defn shutdown-agents
  []
  (when-let [node (find-node)]
    (.shutdown (.getExecutorService node "genesis-agent-send-pool"))))

(defn make-action
  [agent-name f args]
  (reify Runnable
    (run [_]
      (.doRun (make-agent agent-name) f args))
    java.io.Serializable))

(defn agent-error
  [agent]
  (.get (.-error agent)))

(defn restart-agent
  [agent new-state & {:keys [clear-actions]}]
  (.restart agent new-state clear-actions))

(defn await-for
  [timeout-ms & [a & agents]]
  (let [latch (doto (.getCountDownLatch (find-node) (.-agent-name a))
                (.trySetCount (inc (count agents))))
        count-down (fn [agent] (.countDown latch) agent)]
    (doseq [agent (cons a agents)]
      (send agent count-down))
    (.await latch timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)))
