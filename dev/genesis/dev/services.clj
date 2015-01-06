(ns genesis.dev.services
  (:require [genesis.core :refer :all]
            [genesis.cache :as cache]
            [genesis.hazelcast.concurrent :refer [make-id-generator]]
            [genesis.services :refer :all]
            [genesis.commands :refer :all]
            [genesis.netty.codec :as codec]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [cognitect.transit :as transit])
  (:import [io.netty.handler.ssl SslContext]
           [io.netty.handler.ssl.util SelfSignedCertificate]
           [io.netty.handler.codec.http HttpServerCodec HttpObjectAggregator]
           [io.netty.handler.codec.http.websocketx.extensions.compression
            WebSocketServerCompressionHandler]
           [io.netty.handler.codec.http.websocketx
            WebSocketServerHandshaker WebSocketServerHandshakerFactory]))

(defn handle-http-request
  [ctx req]
  (let [loc "wss://127.0.0.1/ws"
        ws-factory (WebSocketServerHandshakerFactory. loc nil true)
        handshaker (.newHandshaker ws-factory req)
        ch (.channel ctx)]
    (if handshaker
      (.handshake handshaker ch req)
      (WebSocketServerHandshakerFactory/sendUnsupportedVersionResponse ch))))

(defn handle-ws-frame
  [ctx frame]
  (.write (.channel ctx) (.text frame)))

(defcommand ws-handler
  [session-store id-generator]
  (channelRead [_ ctx msg]
    (cond
      (instance? io.netty.handler.codec.http.FullHttpRequest msg)
      (handle-http-request ctx msg)
      (instance? io.netty.handler.codec.http.websocketx.WebSocketFrame msg)
      (handle-ws-frame ctx msg)))
  (channelReadComplete [_ ctx]
    (.flush ctx))
  (exceptionCaught [_ ctx cause]
    (.printStackTrace cause)
    (.close ctx)))

(defservice websocket-service
  []
  {:host "127.0.0.1"
   :port 43350}
  (initChannel [_ ch]
    (let [ssc (SelfSignedCertificate.)
          ssl-ctx (SslContext/newServerContext (.certificate ssc)
                                               (.privateKey ssc))]
      (doto (.pipeline ch)
        (.addLast "ssl" (.newHandler ssl-ctx (.alloc ch)))
        (.addLast "codec" (HttpServerCodec.))
        (.addLast "http" (HttpObjectAggregator. 65536))
        (.addLast "ws-compression" (WebSocketServerCompressionHandler.))
        (.addLast "ws-handler" (ws-handler (cache/make-jcache "session-store")
                                           (make-id-generator)))))))
