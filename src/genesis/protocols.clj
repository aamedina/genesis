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

(ns genesis.protocols
  (:require [com.stuartsierra.component :as c])
  (:import com.stuartsierra.component.Lifecycle))

(defprotocol Initializable
  (init [this]))

(defprotocol Reloadable
  (reload [this]))

(extend-protocol Reloadable
  Lifecycle
  (reload [this]
    (c/stop this)
    (when (satisfies? Initializable this)
      (init this))
    (c/start this)))

(defprotocol Configurable
  (config [this])
  (config! [this configuration]))

(defprotocol Server
  (host [this])
  (port [this]))

(defprotocol Client)

(defprotocol Node)

(defprotocol Cluster
  (add-node [this])
  (remove-node [this]))