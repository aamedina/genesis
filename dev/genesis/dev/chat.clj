(ns genesis.dev.chat
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
  (:import [io.netty.handler.ssl SslContext SslHandler]
           [io.netty.handler.ssl.util SelfSignedCertificate]
           [io.netty.util.concurrent GlobalEventExecutor GenericFutureListener]
           [io.netty.channel.group DefaultChannelGroup]))

(defn transit-codec
  []
  (proxy [io.netty.handler.codec.ByteToMessageCodec] []
    (decode [ctx in out]
      (let [in (io.netty.buffer.ByteBufInputStream. in)
            reader (transit/reader in :json)]
        (.add out (transit/read reader))))
    (encode [ctx msg out]
      (let [out (io.netty.buffer.ByteBufOutputStream. out)
            writer (transit/writer out :json)]
        (transit/write writer msg)))))

(defn add-channel
  [channels ctx]
  (-> (.pipeline ctx)
      (.get SslHandler)
      (.handshakeFuture)
      (.addListener (reify GenericFutureListener
                      (operationComplete [_ future]
                        (.add channels (.channel ctx)))))))

(defcommand chat
  [sessions zones channels]
  (channelActive [_ ctx] (add-channel channels ctx))
  (channelRead [_ ctx packet]
    (let [{:keys [session-id zone-id message]} packet
          ;; session (get sessions session-id)
          ;; zone (get zones zone-id)
          ]
      (log/info (.channel ctx) packet)))
  (channelReadComplete [_ ctx]
    (.flush ctx))
  (exceptionCaught [_ ctx cause]
    (.printStackTrace cause)
    (.close ctx)))

(defservice chat-service
  []
  {:host "127.0.0.1"
   :port 43351}
  (initChannel [_ ch]
    (let [ssc (SelfSignedCertificate.)
          ssl-ctx (SslContext/newServerContext (.certificate ssc)
                                               (.privateKey ssc))
          channels (DefaultChannelGroup. GlobalEventExecutor/INSTANCE)]
      (doto (.pipeline ch)
        (.addLast "ssl" (.newHandler ssl-ctx (.alloc ch)))
        (.addLast "codec" (transit-codec))
        (.addLast "handler" (chat (cache/make-jcache "sessions")
                                  (cache/make-jcache "zones")
                                  channels))))))
