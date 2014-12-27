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

(ns genesis.hazelcast.mancenter
  (:require [genesis.protocols :as p]
            [com.stuartsierra.component :as c]
            [genesis.jetty :refer [make-webapp]]
            [environ.core :refer [env]])
  (:import com.hazelcast.core.Hazelcast
           com.hazelcast.core.HazelcastInstance))

(defonce mancenter nil)

(defn make-mancenter
  []
  (make-webapp (env :mancenter-host)
               (env :mancenter-port)
               (env :mancenter-war)))


