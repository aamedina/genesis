(ns genesis.services-test
  (:require [genesis.services :refer :all]
            [genesis.commands :refer :all]
            [genesis.netty.codec :as codec]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import java.net.InetSocketAddress
           java.nio.charset.Charset))

(defcommand echo []
  (channelRead [_ ctx msg]
    (.write ctx (str (str/reverse msg) \newline)))
  (channelReadComplete [_ ctx]
    (.flush ctx))
  (exceptionCaught [_ ctx cause]
    (.printStackTrace cause)
    (.close ctx)))

(defn echo-codec
  []
  (proxy [io.netty.handler.codec.ByteToMessageCodec] []
    (decode [ctx in out]
      (.add out (.toString (.readBytes in (.readableBytes in))
                           (Charset/forName "UTF-8"))))
    (encode [ctx msg out]
      (.writeBytes out (.getBytes msg)))))

(defservice echo-service
  [& options]
  {:host "127.0.0.1"
   :port 9090}
  (initChannel [_ ch]
    (doto (.pipeline ch)
      (.addLast "echo-codec" (echo-codec))
      (.addLast "echo" (echo)))))
