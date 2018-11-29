(ns swarmpit.xhrio
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str]))

(defn- response-header
  [xhrio-header]
  (let [separator-pos (str/index-of xhrio-header ":")
        length (count xhrio-header)]
    {(subs xhrio-header 0 separator-pos)
     (subs xhrio-header (+ separator-pos 2) length)}))

(defn- response-headers
  [xhrio-headers]
  (->> (str/split xhrio-headers #"\r\n")
       (into {} (map response-header))))

(defn- parse-headers
  [xhrio]
  (try
    (-> (.getAllResponseHeaders xhrio)
        (response-headers)
        (keywordize-keys))
    (catch js/Error _ {})))

(defn- parse-body
  [xhrio]
  (try
    (-> (.getResponseJson xhrio)
        (js->clj :keywordize-keys true))
    (catch js/Error _ {})))

(defn response
  [xhrio]
  {:headers (parse-headers xhrio)
   :body    (parse-body xhrio)})
