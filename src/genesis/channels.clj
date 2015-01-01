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

;; Copyright (c) Rich Hickey and contributors. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns genesis.channel
  (:require [genesis.core :refer :all]
            [genesis.buffers :as buffers]
            [clojure.core.async :as a :refer :all
             :exclude [map into reduce merge take partition partition-by chan]]
            [clojure.core.async.impl.protocols :as impl]
            [clojure.core.async.impl.dispatch :as dispatch]
            [clojure.core.async.impl.mutex :as mutex])
  (:import [clojure.lang IFn IPersistentMap PersistentHashMap]
           [com.hazelcast.core HazelcastInstance]
           [com.hazelcast.core IAtomicReference IQueue IList ILock]
           [java.util LinkedList Queue Iterator]
           [java.util.concurrent.locks Lock]))

(set! *warn-on-reflection* true)

(System/setProperty "hazelcast.backpressure.enabled" "true")
(System/setProperty "hazelcast.backpressure.syncwindow" "1024")

(defmacro with-lock
  [lock & body]
  `(let [lock# ~lock]
     (.lock lock#)
     (try
       ~@body
       (finally
         (when (.isLockedByCurrentThread lock#)
           (.unlock lock#))))))

(defmacro assert-unlock [lock test msg]
  `(when-not ~test
     (.unlock ~lock)
     (throw (new AssertionError (str "Assert failed: " ~msg "\n"
                                     (pr-str '~test))))))

(defn box [val]
  (reify clojure.lang.IDeref
    (deref [_] val)))

(definterface MMC
  (cleanup [])
  (abort []))

(deftype ManyToManyChannel [^IList takes
                            ^IList puts
                            ^Queue buf
                            ^IAtomicReference closed
                            ^ILock mutex
                            add!]
  MMC
  (cleanup [_]
    (when-not (.isEmpty takes)
      (let [iter (.iterator takes)]
        (loop [taker (.next iter)]
          (when-not (impl/active? taker)
            (.remove iter))
          (when (.hasNext iter)
            (recur (.next iter))))))
    (when-not (.isEmpty puts)
      (let [iter (.iterator puts)]
        (loop [[putter] (.next iter)]
          (when-not (impl/active? putter)
            (.remove iter))
          (when (.hasNext iter)
            (recur (.next iter)))))))

  (abort [this]
    (let [iter (.iterator puts)]
      (when (.hasNext iter)
        (loop [^Lock putter (.next iter)]
          (.lock putter)
          (let [put-cb (and (impl/active? putter) (impl/commit putter))]
            (.unlock putter)
            (when put-cb
              (dispatch/run (fn [] (put-cb true))))
            (when (.hasNext iter)
              (recur (.next iter)))))))
    (.clear puts)
    (impl/close! this))

  impl/WritePort
  (put!
    [this val handler]
    (when (nil? val)
      (throw (IllegalArgumentException. "Can't put nil on channel")))
    (with-lock mutex
      (.cleanup this)
      (if (.get closed)
        (box false)
        (let [^Lock handler handler]
          (if (and buf (not (impl/full? buf)) (not (.isEmpty takes)))
            (do
              (.lock handler)
              (let [put-cb (and (impl/active? handler) (impl/commit handler))]
                (.unlock handler)
                (if put-cb
                  (let [done? (reduced? (add! buf val))]
                    (if (pos? (count buf))
                      (let [iter (.iterator takes)
                            take-cb (when (.hasNext iter)
                                      (loop [^Lock taker (.next iter)]
                                        (.lock taker)
                                        (let [ret (and (impl/active? taker)
                                                       (impl/commit taker))]
                                          (.unlock taker)
                                          (if ret
                                            (do
                                              (.remove iter)
                                              ret)
                                            (when (.hasNext iter)
                                              (recur (.next iter)))))))]
                        (if take-cb
                          (let [val (impl/remove! buf)]
                            (when done?
                              (.abort this))
                            (.unlock mutex)
                            (dispatch/run (fn [] (take-cb val))))
                          (when done?
                            (.abort this))))
                      (when done?
                        (.abort this)))
                    (box true))
                  nil)))
            (let [iter (.iterator takes)
                  [put-cb take-cb]
                  (when (.hasNext iter)
                    (loop [^Lock taker (.next iter)]
                      (if (< (impl/lock-id handler)
                             (impl/lock-id taker))
                        (do (.lock handler) (.lock taker))
                        (do (.lock taker) (.lock handler)))
                      (let [ret (when (and (impl/active? handler)
                                           (impl/active? taker))
                                  [(impl/commit handler)
                                   (impl/commit taker)])]
                        (.unlock handler)
                        (.unlock taker)
                        (if ret
                          (do
                            (.remove iter)
                            ret)
                          (when (.hasNext iter)
                            (recur (.next iter)))))))]
              (if (and put-cb take-cb)
                (do
                  (.unlock mutex)
                  (dispatch/run (fn [] (take-cb val)))
                  (box true))
                (if (and buf (not (impl/full? buf)))
                  (do
                    (.lock handler)
                    (let [put-cb (and (impl/active? handler)
                                      (impl/commit handler))]
                      (.unlock handler)
                      (if put-cb
                        (let [done? (reduced? (add! buf val))]
                          (when done?
                            (.abort this))
                          (.unlock mutex)
                          (box true))
                        (do (.unlock mutex)
                            nil))))
                  (do
                    (when (impl/active? handler)
                      (assert-unlock mutex
                                     (< (.size puts) impl/MAX-QUEUE-SIZE)
                                     (str "No more than " impl/MAX-QUEUE-SIZE
                                          " pending puts are allowed on a"
                                          " single channel."
                                          " Consider using a windowed buffer."))
                      (.add puts [handler val]))
                    (.unlock mutex)
                    nil)))))))))
  impl/ReadPort
  (take!
    [this handler]
    (.lock mutex)
    (.cleanup this)
    (let [^Lock handler handler
          commit-handler (fn []
                           (.lock handler)
                           (let [take-cb (and (impl/active? handler)
                                              (impl/commit handler))]
                             (.unlock handler)
                             take-cb))]
      (if (and buf (pos? (count buf)))
        (do
          (if-let [take-cb (commit-handler)]
            (let [val (impl/remove! buf)
                  iter (.iterator puts)
                  [done? cbs]
                  (when (.hasNext iter)
                    (loop [cbs []
                           [^Lock putter val] (.next iter)]
                      (.lock putter)
                      (let [cb (and (impl/active? putter) (impl/commit putter))]
                        (.unlock putter)
                        (.remove iter)
                        (let [cbs (if cb (conj cbs cb) cbs)
                              done? (when cb (reduced? (add! buf val)))]
                          (if (and (not done?) (not (impl/full? buf))
                                   (.hasNext iter))
                            (recur cbs (.next iter))
                            [done? cbs])))))]
              (when done?
                (.abort this))
              (.unlock mutex)
              (doseq [cb cbs]
                (dispatch/run #(cb true)))
              (box val))
            (do (.unlock mutex)
                nil)))
        (let [iter (.iterator puts)
              [take-cb put-cb val]
              (when (.hasNext iter)
                (loop [[^Lock putter val] (.next iter)]
                  (if (< (impl/lock-id handler) (impl/lock-id putter))
                    (do (.lock handler) (.lock putter))
                    (do (.lock putter) (.lock handler)))
                  (let [ret (when (and (impl/active? handler)
                                       (impl/active? putter))
                              [(impl/commit handler) (impl/commit putter) val])]
                    (.unlock handler)
                    (.unlock putter)
                    (if ret
                      (do
                        (.remove iter)
                        ret)
                      (when-not (impl/active? putter)
                        (.remove iter)
                        (when (.hasNext iter)
                          (recur (.next iter))))))))]
          (if (and put-cb take-cb)
            (do
              (.unlock mutex)
              (dispatch/run #(put-cb true))
              (box val))
            (if (.get closed)
              (do
                (when buf (add! buf))
                (let [has-val (and buf (pos? (count buf)))]
                  (if-let [take-cb (commit-handler)]
                    (let [val (when has-val (impl/remove! buf))]
                      (.unlock mutex)
                      (box val))
                    (do
                      (.unlock mutex)
                      nil))))
              (do
                (assert-unlock mutex
                               (< (.size takes) impl/MAX-QUEUE-SIZE)
                               (str "No more than " impl/MAX-QUEUE-SIZE
                                    " pending takes are allowed on a"
                                    " single channel."))
                (.add takes handler)
                (.unlock mutex)
                nil)))))))

  impl/Channel
  (closed? [_] (.get closed))
  (close!
    [this]
    (.lock mutex)
    (.cleanup this)
    (if (.get closed)
      (do
        (.unlock mutex)
        nil)
      (do
        (.set closed true)
        (when (and buf (.isEmpty puts))
          (add! buf))
        (let [iter (.iterator takes)]
          (when (.hasNext iter)
            (loop [^Lock taker (.next iter)]
              (.lock taker)
              (let [take-cb (and (impl/active? taker) (impl/commit taker))]
                (.unlock taker)
                (when take-cb
                  (let [val (when (and buf (pos? (count buf)))
                              (impl/remove! buf))]
                    (dispatch/run (fn [] (take-cb val)))))
                (.remove iter)
                (when (.hasNext iter)
                  (recur (.next iter)))))))
        (.unlock mutex)
        nil))))

(defn- ex-handler [ex]
  (-> (Thread/currentThread)
      .getUncaughtExceptionHandler
      (.uncaughtException (Thread/currentThread) ex))
  nil)

(defn- handle [buf exh t]
  (let [else ((or exh ex-handler) t)]
    (if (nil? else)
      buf
      (impl/add! buf else))))

(defn chan
  ([channel-name]
   (chan channel-name nil))
  ([channel-name buf]
   (chan channel-name buf nil))
  ([channel-name buf xform]
   (chan channel-name buf xform nil))
  ([channel-name buf xform exh] 
   (let [channel-name (name channel-name)
         node (find-node)
         takes (.getList node (str channel-name "-takes"))
         puts (.getList node (str channel-name "-puts"))
         closed (.getAtomicReference node (str channel-name "-closed?"))
         mutex (.getLock node (str channel-name "-lock"))]
     (ManyToManyChannel. takes puts buf (if (.isNull closed)
                                          (doto closed
                                            (.set false))
                                          closed)
                         mutex
                         (let [add! (if xform (xform impl/add!) impl/add!)]
                           (fn
                             ([buf]
                              (try
                                (add! buf)
                                (catch Throwable t
                                  (handle buf exh t))))
                             ([buf val]
                              (try
                                (add! buf val)
                                (catch Throwable t
                                  (handle buf exh t))))))))))
