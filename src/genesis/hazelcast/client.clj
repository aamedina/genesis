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

(ns genesis.hazelcast.client
  (:require [genesis.protocols :as p]
            [com.stuartsierra.component :as c])
  (:import com.hazelcast.core.HazelcastInstance
           com.hazelcast.core.IMap
           com.hazelcast.client.config.ClientConfig
           com.hazelcast.client.HazelcastClient))

(defrecord Client [client config f]
  p/Client

  c/Lifecycle
  (start [this]
    (if client
      this
      (try
        (let [config (ClientConfig.)
              client (HazelcastClient/tnewHazelcastClient config)]
          (f client config)
          (assoc this
            :config config
            :client client))
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))))
  (stop [this]
    (if client
      (try
        (assoc this :client nil :config nil)
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))
      this)))

(defn make-client
  [f]
  (HazelcastClient. nil nil f))
