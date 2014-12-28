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

(ns genesis.hazelcast.node-test
  (:require [genesis.hazelcast.node :as node]))

(defn getting-started
  [node]
  (let [m (doto (.getMap node "customers")
            (.put 1 "Joe")
            (.put 1 "Ali")
            (.put 1 "Avi"))
        q (doto (.getQueue node "customers")
            (.offer "Tom")
            (.offer "Mary")
            (.offer "Jane"))]
    
    (println "Customer with key 1: " (.get m 1))
    (println "Map Size: " (.size m))

    (println "First customer: " (.poll q))
    (println "Second customer: " (.peek q))
    (println "Queue size: " (.size q))))
