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

(ns genesis.hazelcast.cluster-test
  (:require [genesis.protocols :as p]
            [genesis.hazelcast.cluster :as clst]
            [genesis.hazelcast.node :as n]
            [genesis.hazelcast.client :as cl]
            [genesis.hazelcast.node-test :as n-test]
            [genesis.hazelcast.client-test :as cl-test]
            [com.stuartsierra.component :as c]))

(defonce cluster nil)

(defn setup-test-cluster
  []
  (alter-var-root #'cluster (fn [_]
                              (clst/system {:f n-test/getting-started
                                            :num-nodes 2}))))

(defn getting-started
  []
  (setup-test-cluster)
  (let [cluster (alter-var-root #'cluster c/start-system)
        client (c/start (cl/make-client cl-test/getting-started))]
    (c/stop client)
    cluster))
