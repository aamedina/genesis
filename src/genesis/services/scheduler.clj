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

(ns genesis.services.scheduler
  (:require [genesis.protocols :as p]
            [clojure.core.cache :as cache :refer [defcache]]))

(defn scheduler?
  [x]
  (satisfies? p/Scheduler x))

(defmacro defscheduler
  [name [cache & fields] & specs]
  `(deftype ~name [~cache ~@fields]
     Object
     (toString [_] (str ~cache))
     ~@specs))

(defscheduler RoundRobinScheduler [cache]
  p/Scheduler
  (cancel-task [_ k])
  (cancel-all [_])
  (flush-tasks [_])
  (schedule [_ k v delay-ms periodic?]))

(defscheduler FairQueuingScheduler [cache]
  p/Scheduler
  (cancel-task [_ k])
  (cancel-all [_])
  (flush-tasks [_])
  (schedule [_ k v delay-ms periodic?]))

(defscheduler ProportionallyFairScheduler [cache]
  p/Scheduler
  (cancel-task [_ k])
  (cancel-all [_])
  (flush-tasks [_])
  (schedule [_ k v delay-ms periodic?]))

(defscheduler MaximumThroughputScheduler [cache]
  p/Scheduler
  (cancel-task [_ k])
  (cancel-all [_])
  (flush-tasks [_])
  (schedule [_ k v delay-ms periodic?]))
