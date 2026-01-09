(ns swarmpit.influxdb.mapper
  (:require [clojure.string :as str]
            [swarmpit.utils :refer [as-MiB]]
            [clj-time.format :as format])
  (:import [org.joda.time DateTimeZone]))

(defn ->task-tags [task-name host-name]
  (let [segments (drop 1 (str/split task-name #"/|\."))]
    (if (= 1 (count segments))
      nil
      {:task    (str (first segments) "." (second segments))
       :service (first segments)
       :host    host-name})))

(defn ->host-tags [host-name]
  {:host host-name})

(defn- convert-to-local-timezone
  "Convert UTC timestamp string to JVM's default (local) timezone"
  [utc-timestamp]
  (when utc-timestamp
    (try
      (let [ts-str (if (string? utc-timestamp) 
                     utc-timestamp 
                     (str utc-timestamp))
            ;; Try parsing with date-time formatter first (handles with milliseconds)
            dt (try
                 (format/parse (format/formatters :date-time) ts-str)
                 (catch Exception _
                   ;; If that fails, try date-time-no-ms
                   (try
                     (format/parse (format/formatters :date-time-no-ms) ts-str)
                     (catch Exception _
                       ;; Last resort: try basic ISO format
                       (format/parse (format/formatters :basic-date-time) ts-str)))))
            default-tz (DateTimeZone/getDefault)
            tz-dt (.withZone dt default-tz)
            ;; Format the DateTime in the local timezone with offset
            ;; We need to manually format with timezone offset since :date-time converts to UTC
            year (.getYear tz-dt)
            month (.getMonthOfYear tz-dt)
            day (.getDayOfMonth tz-dt)
            hour (.getHourOfDay tz-dt)
            minute (.getMinuteOfHour tz-dt)
            second (.getSecondOfMinute tz-dt)
            tz-zone (.getZone tz-dt)
            offset-millis (.getOffset tz-zone (.getMillis tz-dt))
            offset-hours (quot offset-millis (* 60 60 1000))
            offset-minutes (quot (rem offset-millis (* 60 60 1000)) (* 60 1000))
            offset-str (if (neg? offset-hours)
                         (clojure.core/format "-%02d:%02d" (Math/abs offset-hours) (Math/abs offset-minutes))
                         (clojure.core/format "+%02d:%02d" offset-hours offset-minutes))
            result (clojure.core/format "%04d-%02d-%02dT%02d:%02d:%02d%s" year month day hour minute second offset-str)]
        result)
      (catch Exception _
        ;; If all parsing fails, return original timestamp
        utc-timestamp))))

(defn ->task-ts [series]
  (let [values (get series "values")
        tags (get series "tags")]
    {:task    (get tags "task")
     :service (get tags "service")
     :time    (into [] (map #(convert-to-local-timezone (first %)) values))
     :cpu     (into [] (map second values))
     :memory  (into [] (->> (map #(nth % 2) values)
                            (map #(when (some? %)
                                    (as-MiB %)))))}))

(defn ->host-ts [series]
  (let [values (get series "values")
        tags (get series "tags")]
    {:name   (get tags "host")
     :time   (into [] (map #(convert-to-local-timezone (first %)) values))
     :cpu    (into [] (map second values))
     :memory (into [] (map #(nth % 2) values))}))

(defn ->service-max-usage [series]
  (->> (map #(let [values (first (get % "values"))
                   service (get-in % ["tags" "service"])]
               (hash-map :service service
                         :cpu (second values)
                         :memory (nth values 2))) series)
       (set)))