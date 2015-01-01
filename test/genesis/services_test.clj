(ns genesis.services-test
  (:require [genesis.services :refer :all]
            [genesis.commands :refer :all])
  (:import java.net.InetSocketAddress))

(def some-udp-server
  {:host "127.0.0.1"
   :port 9090
   :protocol :udp})

(def some-tcp-server
  {:host "127.0.0.1"
   :port 9091
   :protocol :tcp})

(defservice basic-udp-service
  :remote some-udp-server)

(defservice basic-tcp-service
  :remote some-tcp-server)

(defcommand echo
  :service basic-tcp-service
  identity)

(defcommand ping
  :service basic-udp-service
  (constantly "pong"))
