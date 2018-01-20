(ns swarmpit.xhrio
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str]))

(defn- response-header
  [xhrio-header]
  (let [separator-pos (str/index_of xhrio-header ":")
        length (count xhrio-header)]
    {(subs xhrio-header 0 separator-pos)
     (subs xhrio-header (+ separator-pos 2) length)}))

(defn- response-headers
  [xhrio-headers]
  (->> (str/split xhrio-headers #"\r\n")
       (map #(response-header %))
       (into {})))

(defn response
  [xhrio]
  {:headers (-> (.getAllResponseHeaders xhrio)
                (response-headers)
                (keywordize-keys))
   :body    (try
              (-> (.getResponseJson xhrio)
                  (js->clj)
                  (keywordize-keys))
              (catch js/Error _
                {}))})
