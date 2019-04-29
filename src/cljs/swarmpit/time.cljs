(ns swarmpit.time
  (:require [clojure.contrib.humanize :as humanize]
            [cljs-time.coerce :refer [to-epoch]]
            [cljs-time.core :refer [after? to-default-time-zone now minus minutes]]
            [cljs-time.format :as format]
            [clojure.string :as str]))

(def docker-format
  (format/formatters :date-time-no-ms))

(def simple-format
  (format/formatters :mysql))

(defn- trim-ms
  [datetime]
  (str/replace datetime #"\.(.+)(Z|\+.*|-.*)$" "$2"))

(defn parse
  [datetime]
  (->> (trim-ms datetime)
       (format/parse docker-format)))

(defn to-unix
  [datetime]
  (let [dt (parse datetime)]
    (and dt (quot dt 1000))))

(defn to-unix-past
  [minutes-to-history]
  (let [dt (minus
             (now)
             (minutes minutes-to-history))]
    (and dt (quot dt 1000))))

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
         (catch :default _
           false))))