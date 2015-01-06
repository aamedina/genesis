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
           [io.netty.buffer ByteBuf Unpooled]))

(set! *warn-on-reflection* true)

(defmacro byte-to-message-decoder
  [[args & body] & specs]
  (let [interfaces (filter symbol? specs)
        impls (remove symbol? specs)]
    `(proxy [io.netty.handler.codec.ByteToMessageDecoder ~@interfaces] []
       (decode ~args ~@body)
       ~@impls)))

(defmacro message-to-byte-encoder
  [[args & body] & specs]
  (let [interfaces (filter symbol? specs)
        impls (remove symbol? specs)]
    `(proxy [io.netty.handler.codec.MessageToByteEncoder ~@interfaces] []
       (encode ~args ~@body)
       ~@impls)))

(defmacro message-to-message-decoder
  [[args & body] & specs]
  (let [interfaces (filter symbol? specs)
        impls (remove symbol? specs)]
    `(proxy [io.netty.handler.codec.MessageToMessageDecoder ~@interfaces] []
       (decode ~args ~@body)
       ~@impls)))

(defmacro message-to-message-encoder
  [[args & body] & specs]
  (let [interfaces (filter symbol? specs)
        impls (remove symbol? specs)]
    `(proxy [io.netty.handler.codec.MessageToMessageEncoder ~@interfaces] []
       (encode ~args ~@body)
       ~@impls)))

(defmacro byte-to-message-codec
  [& specs]
  (let [interfaces (filter symbol? specs)
        impls (remove symbol? specs)]
    `(proxy [io.netty.handler.codec.ByteToMessageCodec ~@interfaces] []
       ~@impls)))

(defmacro message-to-message-codec
  [& specs]
  (let [interfaces (filter symbol? specs)
        impls (remove symbol? specs)]
    `(proxy [io.netty.handler.codec.MessageToMessageCodec ~@interfaces] []
       ~@impls)))
