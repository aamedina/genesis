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

(ns genesis.netty.channels
  (:import [io.netty.channel ChannelHandler]))

(defonce ^:private default-channel-handler-methods
  '[(handlerAdded [_ ctx#])
    (handlerRemoved [_ ctx#])
    (exceptionCaught [_ ctx# cause#] (.fireExceptionCaught ctx# cause#))
    (channelRegistered [_ ctx#] (.fireChannelRegistered ctx#))
    (channelUnregistered [_ ctx#] (.fireChannelUnregistered ctx#))
    (channelActive [_ ctx#] (.fireChannelActive ctx#))
    (channelInactive [_ ctx#] (.fireChannelInactive ctx#))
    (channelRead [_ ctx#] (.fireChannelRead ctx#))
    (channelReadComplete [_ ctx#] (.fireChannelReadComplete ctx#))
    (userEventTriggered [_ ctx#] (.fireUserEventTriggered ctx#))
    (channelWritabilityChanged [_ ctx#] (.fireChannelWritabilityChanged ctx#))
    (bind [_ ctx# local-address# promise#] (.bind ctx# local-address# promise#))
    (connect [_ ctx# remote# local# pr#] (.connect ctx# remote# local# pr#))
    (disconnect [_ ctx# pr#] (.disconnect ctx# pr#))
    (close [_ ctx# pr#] (.close ctx# pr#))
    (deregister [_ ctx# pr#] (.deregister ctx# pr#))
    (read [_ ctx#] (.read ctx#))
    (write [_ ctx# msg# pr#] (.write ctx# msg# pr#))
    (flush [_ ctx#] (.flush ctx#))])

(defn- make-method-map
  [specs]
  (into [] (map (fn [[k [method]]] method))
        (merge (group-by first default-channel-handler-methods)
               (group-by first specs))))

(defmacro channel-handler
  [& specs]
  (let [impls (make-method-map (take-while list? specs))
        specs (drop-while list? specs)]
    `(reify ChannelHandler
       ~@impls
       ~@specs)))
