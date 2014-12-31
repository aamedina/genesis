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

(ns genesis.buffers
  (:require [genesis.core :refer :all]
            [clojure.core.async.impl.protocols :as impl])
  (:import [java.util LinkedList Queue]
           [com.hazelcast.core IAtomicReference IQueue IList ILock]))

(set! *warn-on-reflection* true)

(deftype FixedBuffer [^IQueue buf ^long n]
  impl/Buffer
  (full? [this]
    (>= (.size buf) n))
  (remove! [this]
    (.poll buf))
  (add!* [this itm]
    (.offer buf itm)
    this)
  clojure.lang.Counted
  (count [this]
    (.size buf)))

(defn fixed-buffer [channel-name ^long n]
  (let [buf (.getQueue (find-node) (str channel-name "-buf"))]
    (FixedBuffer. buf n)))


(deftype DroppingBuffer [^IQueue buf ^long n]
  impl/UnblockingBuffer
  impl/Buffer
  (full? [this]
    false)
  (remove! [this]
    (.poll buf))
  (add!* [this itm]
    (when-not (>= (.size buf) n)
      (.offer buf itm))
    this)
  clojure.lang.Counted
  (count [this]
    (.size buf)))

(defn dropping-buffer [channel-name n]
  (let [buf (.getQueue (find-node) (str channel-name "-buf"))]
    (DroppingBuffer. buf n)))

(deftype SlidingBuffer [^IQueue buf ^long n]
  impl/UnblockingBuffer
  impl/Buffer
  (full? [this]
    false)
  (remove! [this]
    (.poll buf))
  (add!* [this itm]
    (when (= (.size buf) n)
      (impl/remove! this))
    (.offer buf itm)
    this)
  clojure.lang.Counted
  (count [this]
    (.size buf)))

(defn sliding-buffer [channel-name n]
  (let [buf (.getQueue (find-node) (str channel-name "-buf"))]
    (SlidingBuffer. buf n)))
