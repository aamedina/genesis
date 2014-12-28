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

(ns genesis.jetty
  (:require [genesis.protocols :as p]
            [com.stuartsierra.component :as c]
            [environ.core :refer [env]])
  (:import org.eclipse.jetty.server.Server
           org.eclipse.jetty.webapp.WebAppContext
           java.net.InetSocketAddress))

(defrecord JettyWebApp [server webapp addr war-file]
  c/Lifecycle
  (start [this]
    (if server
      this
      (try
        (let [webapp (doto (WebAppContext.)
                       (.setContextPath "/")
                       (.setWar war-file))]
          (assoc this
            :webapp webapp
            :server (doto (Server. addr)
                      (.setHandler webapp)
                      (.start))))
        
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))))
  (stop [this]
    (if server
      (try
        (.stop webapp)
        (.stop server)
        (assoc this :server nil :webapp nil)
        (catch Throwable t
          (assoc this
            :error (ex-info (.getMessage t) {} t))))
      this)))

(defn make-webapp
  [host port war-file]
  (JettyWebApp. nil nil (InetSocketAddress. host port) war-file))
