(ns swarmpit.url
  (:require [clojure.string :refer [split]]
            [swarmpit.router :as router]))

(defn dispatch!
  [url]
  (router/set-location url))

(defn url
  "Get current URL address"
  []
  (-> js/document .-location .-href))

(defn query-string
  "Parse query string from URL params"
  []
  (->> (split (url) #"\?")
       (second)))
