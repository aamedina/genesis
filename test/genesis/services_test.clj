(ns genesis.services-test
  (:require [genesis.services :refer :all])
  (:import java.net.InetSocketAddress))

(comment
  (defremote some-udp-server
    {:host "127.0.0.1"
     :port 9090
     :protocol :udp})

  (defremote some-tcp-server
    {:host "127.0.0.1"
     :port 9091
     :protocol :tcp})

  (defservice basic-service
    :load-balancer false
    :cache false
    :store false)

  (def echo-service
    (extend-service basic-service
      :remote some-tcp-server))
  
  (defcommand echo
    :service echo-service
    identity)

  (def some-udp-service
    (extend-service basic-service
      :remote some-tcp-server))

  (defcommand ping
    :service some-udp-service
    
    (constantly "pong")))
