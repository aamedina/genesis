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

(ns genesis.services.load-balancer
  (:require [genesis.protocols :as p]
            [genesis.services.scheduler :as s])
  (:import java.util.concurrent.atomic.AtomicReference
           (java.util.concurrent CountDownLatch
                                 Executor
                                 ExecutorService
                                 Executors
                                 ThreadFactory
                                 TimeUnit)))

(defn load-balancer?
  [x]
  (satisfies? p/LoadBalancer x))

(deftype BasicLoadBalancer [scheduler]
  p/LoadBalancer)

(defn make-basic-load-balancer
  [scheduler]
  {:pre [(s/scheduler? scheduler)]}
  (BasicLoadBalancer. scheduler))

(deftype ClusterAwareLoadBalancer [node scheduler]
  p/ClusterAware
  
  p/LoadBalancer)

(defn make-cluster-aware-load-balancer
  ([node]
   (make-cluster-aware-load-balancer node (s/->RoundRobinScheduler {})))
  ([node scheduler]
   {:pre [(s/scheduler? scheduler)]}
   (ClusterAwareLoadBalancer. node scheduler)))
