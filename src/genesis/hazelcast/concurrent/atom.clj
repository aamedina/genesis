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

(ns genesis.hazelcast.concurrent.atom
  (:import com.hazelcast.core.IAtomicReference))

(deftype DistributedAtom [^com.hazelcast.core.IAtomicReference state]
  clojure.lang.IAtom
  (swap [_ f])
  (swap [_ f x])
  (swap [_ f x y])
  (swap [_ f x y args])
  (compareAndSet [_ oldval newval])
  (reset [_ newval])

  clojure.lang.IRef
  (setValidator [_ f])
  (getValidator [_])
  (getWatches [_])
  (addWatch [_ k f])
  (removeWatch [_ k]))

(defn make-distributed-atom
  [node x & options]
  (let [ref (.getAtomicReference node (name (gensym "atom")))]
    (.set ref x)
    (DistributedAtom. ref)))
