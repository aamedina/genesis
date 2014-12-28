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

(ns genesis.hazelcast.concurrent
  (:require [genesis.core :refer :all])
  (:import [com.hazelcast.core Hazelcast ICountDownLatch HazelcastInstance]))

(defn make-id-generator
  []
  (.getIdGenerator (find-node) "IdGenerator"))

(defn genstr
  ([] (genstr nil))
  ([prefix]
   (str prefix (.newId (make-id-generator (find-node))))))

(defn make-countdown-latch
  ([] (make-countdown-latch 1))
  ([^long count]
   (let [latch (.getCountDownLatch (find-node) (genstr "CountDownLatch"))]
     (when (pos? count)
       (.trySetCount latch count))
     latch)))

(defn make-executor-service
  []
  (.getExecutorService (find-node) (genstr "ExecutorService")))


