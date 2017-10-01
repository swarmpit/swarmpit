(ns swarmpit.time
  (:require [clojure.contrib.humanize :as humanize]
            [cljs-time.core :refer [after? to-default-time-zone now]]
            [cljs-time.format :as format]))

(def docker-format
  (format/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ"))

(def simple-format
  (format/formatters :mysql))

(defn parse
  [datetime]
  (format/parse docker-format datetime))

(defn- shift-future-date
  [date]
  (if (after? date (now))
    (now)
    date))

(defn humanize
  [datetime]
  (->> datetime
       (parse)
       (shift-future-date)
       (humanize/datetime)))

(defn simplify
  [datetime]
  (->> datetime
       (parse)
       (to-default-time-zone)
       (format/unparse simple-format)))