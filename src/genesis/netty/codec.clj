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

(ns genesis.netty.codec
  (:require [genesis.netty.channels :refer [channel-handler]]
            [clojure.tools.logging :as log])
  (:import [io.netty.channel ChannelHandler]
           [io.netty.buffer ByteBuf Unpooled]
           [io.netty.handler.codec DecoderException]
           [io.netty.util.internal RecyclableArrayList StringUtil]))

(set! *warn-on-reflection* true)

(defprotocol Decoder
  (decode [_ ctx in out]))

(defmacro byte-to-message-decoder
  [decoder & specs]
  `(let [cumulation# (volatile! nil)]
     (channel-handler
       (handlerRemoved [_ ctx#]
         (when-let [^ByteBuf buf# @cumulation#]
           (when (.isReadable buf#)
             (.fireChannelRead ctx# (.readBytes buf# (.readableBytes buf#)))))
         (vreset! cumulation# nil)
         (.fireChannelReadComplete ctx#))
       (channelRead [this# ctx# msg#]
         (if (instance? ByteBuf msg#)
           (let [out# (RecyclableArrayList/newInstance)]
             (try
               (if (nil? @cumulation#)
                 (vreset! cumulation# msg#)
                 (let [buf# @cumulation#]
                   (when (or (> (.writerIndex buf#)
                                (- (.maxCapacity buf#)
                                   (.readableBytes msg#)))
                             (> (.refCnt buf#) 1))
                     (let [new# (.. ctx#
                                    (alloc)
                                    (buffer (+ (.readableBytes buf#)
                                               (.readableBytes msg#))))]
                       (.writeBytes new#)
                       (.release buf#)
                       (vreset! cumulation# new#)))
                   (.writeBytes buf# data#)
                   (.release msg#)))
               (let [buf# @cumulation#]
                 (loop []
                   (when (.isReadable buf#)
                     (let [out-size# (.size out#)
                           old-input-len# (.readableBytes in#)]
                       (.decode this# ctx# buf# out#))
                     (when-not (.isRemoved ctx#)
                       (cond
                         (== out-size# (.size out#))
                         (when-not (== out-input-len# (.readableBytes buf#))
                           (recur))

                         (== out-input-len# (.readableBytes buf#))
                         (throw (DecoderException. ""))

                         :else (recur))))))
               (catch DecoderException e#
                 (throw e#))
               (catch Throwable e#
                 (throw (DecoderException. e#)))
               (finally)))
           (.fireChannelRead ctx# msg#)))
       (channelReadComplete [_ ctx#]
         (.fireChannelReadComplete ctx#))
       (channelInactive [_ ctx#])

       genesis.netty.codec.Decoder
       (decode ~decoder)
       
       ~@specs)))
