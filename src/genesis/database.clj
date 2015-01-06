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

(ns genesis.database
  (:refer-clojure :exclude [sync filter])
  (:require [datomic.api :refer :all :as api]
            [environ.core :refer [env]]))

;; (defonce ^:dynamic *uri* (env :datomic-uri))
;; (defonce ^:dynamic *connection* (do (create-database *uri*) (connect *uri*)))
;; (defonce ^:dynamic *db* (db *connection*))
