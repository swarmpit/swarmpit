(ns swarmpit.time
  (:require [clojure.contrib.humanize :as humanize]
            [clojure.string :as str]
            [cljs-time.core :refer [after? to-default-time-zone now]]
            [cljs-time.format :as format]))

(def docker-format
  (format/formatters :date-time-no-ms))

(def simple-format
  (format/formatters :mysql))

(defn- trim-ms
  [datetime]
  (str/replace datetime #"\.(.+)Z$" "Z"))

(defn parse
  [datetime]
  (->> (trim-ms datetime)
       (format/parse docker-format)))

(defn- shift-future-date
  [date]
  (if (after? date (now))
    (now)
    date))

(defn humanize
  [datetime]
  (->> (parse datetime)
       (shift-future-date)
       (humanize/datetime)))

(defn simplify
  [datetime]
  (->> (parse datetime)
       (to-default-time-zone)
       (format/unparse simple-format)))

(defn valid?
  [datetime]
  (if (= datetime "0001-01-01T00:00:00Z")
    false
    (try (do (parse datetime)
             true)
         (catch :default e
           false))))