(ns swarmpit.url
  (:require [clojure.string :refer [split]]))

(defn dispatch!
  [url]
  (set! js/document.location url))

(defn url
  "Get current URL address"
  []
  (-> js/document .-location .-href))

(defn query-string
  "Parse query string from URL params"
  []
  (->> (split (url) #"\?")
       (second)))

(defn query-params
  "Parse URL parameters into a hashmap"
  []
  (->> (split (query-string) #"&")
       (map #(split % #"="))
       (map (fn [[k v]] [(keyword k) v]))
       (into {})))
