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

  (def basic-tcp-service
    (extend-service basic-service
      :remote some-tcp-server))
  
  (defcommand echo
    :service basic-tcp-service
    identity)

  (defn setup-service
    [{:keys [remote codec pipeline] :as service}]
    )

  (def basic-udp-service
    (extend-service basic-service
      :remote basic-udp-server
      :codec nil))

  (defcommand ping
    :service basic-udp-service
    (constantly "pong")))
