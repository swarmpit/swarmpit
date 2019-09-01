(ns swarmpit.time
  (:require
    #?@(:clj  [[clojure.string :as str]
               [clojure.contrib.humanize :as humanize]
               [clj-time.coerce :refer [to-epoch]]
               [clj-time.core :refer [after? now minus minutes]]
               [clj-time.format :as format]]
        :cljs [[clojure.string :as str]
               [clojure.contrib.humanize :as humanize]
               [cljs-time.core :refer [after? to-default-time-zone now minus minutes]]
               [cljs-time.format :as format]])))

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

#?(:clj
   (defn to-unix
     [datetime]
     (to-epoch datetime))
   :cljs
   (defn to-unix
     [datetime]
     (let [dt (parse datetime)]
       (and dt (quot dt 1000)))))

#?(:clj
   (defn to-unix-past
     [minutes-to-history]
     (to-epoch
       (minus
         (now)
         (minutes minutes-to-history))))
   :cljs
   (defn to-unix-past
     [minutes-to-history]
     (let [dt (minus
                (now)
                (minutes minutes-to-history))]
       (and dt (quot dt 1000)))))

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

#?(:cljs
   (defn simplify
     [datetime]
     (->> (parse datetime)
          (to-default-time-zone)
          (format/unparse simple-format))))

#?(:cljs
   (defn in-past-string
     [minutes-to-history]
     (format/unparse
       (format/formatters :date-time)
       (minus
         (now)
         (minutes minutes-to-history)))))

(defn valid?
  [datetime]
  (if (= datetime "0001-01-01T00:00:00Z")
    false
    (try (do (parse datetime)
             true)
         #?(:clj  (catch Exception _ false)
            :cljs (catch :default _ false)))))

(defn since-to-minutes [since]
  (case since
    "1m" 1
    "15m" 15
    "30m" 30
    "60m" 60
    "4h" 240
    "8h" 480
    "12h" 720
    "24h" 1440
    0))