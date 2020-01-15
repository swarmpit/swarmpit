(ns swarmpit.influxdb.mapper
  (:require [clojure.string :as str]
            [swarmpit.utils :refer [as-MiB]]))

(defn ->task-tags [task-name host-name]
  (let [segments (drop 1 (str/split task-name #"/|\."))]
    (if (= 1 (count segments))
      nil
      {:task    (str (first segments) "." (second segments))
       :service (first segments)
       :host    host-name})))

(defn ->host-tags [host-name]
  {:host host-name})

(defn ->task-ts [series]
  (let [values (get series "values")
        tags (get series "tags")]
    {:task    (get tags "task")
     :service (get tags "service")
     :time    (into [] (map first values))
     :cpu     (into [] (map second values))
     :memory  (into [] (->> (map #(nth % 2) values)
                            (map #(when (some? %)
                                    (as-MiB %)))))}))

(defn ->host-ts [series]
  (let [values (get series "values")
        tags (get series "tags")]
    {:name   (get tags "host")
     :time   (into [] (map first values))
     :cpu    (into [] (map second values))
     :memory (into [] (map #(nth % 2) values))}))

(defn ->service-max-usage [series]
  (->> (map #(let [values (first (get % "values"))
                   service (get-in % ["tags" "service"])]
               (hash-map :service service
                         :cpu (second values)
                         :memory (nth values 2))) series)
       (set)))