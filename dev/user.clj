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

(ns user
  (:require [genesis.core :refer :all]
            [genesis.netty :as netty]
            [genesis.protocols :as p]
            [genesis.hazelcast.cluster :as clst]
            [genesis.hazelcast.node :as n]
            [genesis.hazelcast.client :as cl]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.logging :as log]
            [criterium.core :refer [quick-bench]])
  (:import [com.hazelcast.core Hazelcast HazelcastInstance]))

(defonce system
  (clst/system {:f (fn [node]) :num-nodes 2}))

(defn start
  []
  (alter-var-root #'system c/start-system))

(defn stop
  []
  (alter-var-root #'system c/stop-system))

(defn reset
  []
  (stop)
  (repl/refresh-all :after 'user/go))

(defn go
  []
  (start))
