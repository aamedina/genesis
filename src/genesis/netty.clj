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

(ns genesis.netty
  (:require [genesis.core :refer :all]
            [genesis.hazelcast.concurrent.atom :as atom]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log])
  (:import java.net.InetSocketAddress
           [io.netty.bootstrap Bootstrap ServerBootstrap]
           [io.netty.channel ChannelFuture ChannelInitializer ChannelOption]
           [io.netty.channel ChannelFutureListener]
           [io.netty.channel ChannelPipeline EventLoopGroup]
           [io.netty.channel.nio NioEventLoopGroup]
           [io.netty.channel.socket SocketChannel]
           [io.netty.channel.socket.nio NioServerSocketChannel]
           [io.netty.channel.socket.nio NioDatagramChannel]
           [io.netty.handler.logging LogLevel LoggingHandler]
           [io.netty.handler.ssl SslContext]
           [io.netty.handler.ssl.util SelfSignedCertificate]))

(defn- default-options
  []
  {ChannelOption/SO_BACKLOG 100})

(defn setup-tcp-server
  [server]
  (let [boss-group (NioEventLoopGroup.)
        worker-group (NioEventLoopGroup.)]
    (assoc server
      :bootstrap (doto (Bootstrap.)
                   (.group boss-group worker-group)
                   (.channel NioServerSocketChannel)))))

(defn setup-udp-server
  [server]
  (let [group (NioEventLoopGroup.)]
    (assoc server
      :bootstrap (doto (Bootstrap.)
                   (.group group)
                   (.channel NioDatagramChannel)))))

(defn log-future-error
  [channel-future]
  (let [e (.cause channel-future)]
    (log/error e (.getMessage e))))

(defn bind
  [server]
  (let [addr (InetSocketAddress. (or (:host server) "127.0.0.1") (:port server))
        bind-future (.bind (:bootstrap server) addr)]
    (.addListener bind-future (reify ChannelFutureListener
                                (operationComplete [_ channel-future]
                                  (when-not (.isSuccess channel-future)
                                    (log-future-error channel-future)))))
    server))

(defrecord NettyServer [host port protocol handler bootstrap options]
  c/Lifecycle
  (start [this]
    (if bootstrap
      this
      (let [server (doto (case protocol
                           :udp (setup-udp-server this)
                           (setup-tcp-server this))
                     (.handler handler))]
        (doseq [[k v] (merge (default-options) options)]
          (.option (:bootstrap server) k v))
        (bind server))))
  (stop [this]
    (if bootstrap
      (try
        (when-not (.isShuttingDown (.group bootstrap))
          (case protocol
            :udp (.shutdownGracefully (.group bootstrap))
            (do (.shutdownGracefully (.group bootstrap))
                (.shutdownGracefully (.childGroup bootstrap)))))
        (assoc this :bootstrap nil))
      this)))

(defn netty-server
  [config]
  (map->NettyServer config))

(defn find-remote
  [remote]
  (let [m (.getMap (find-node) "remotes")]
    (.get m remote)))

(defmulti create-remote (fn [sym {:keys [provider]}] provider))

(defmethod create-remote :default
  [sym config]
  (or (find-remote sym)      
      (let [m (.getMap (find-node) "remotes")
            remote (netty-server config)]
        (.put m sym remote)
        remote)))

(defn extend-remote
  [sym & {:as config}]
  (let [m (.getMap (find-node) "remotes")
        remote (.get m sym)]
    (.put m (into remote config))
    (.get m sym)))
