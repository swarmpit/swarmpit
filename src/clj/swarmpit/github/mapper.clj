(ns swarmpit.github.mapper
  (:require [clojure.string :as str]))

(defn ->package-name [response type]
  (let [nodes (get-in response [type :packages :nodes])]
    (map #(str (get-in % [:repository :owner :login])
               "/"
               (:name %)) nodes)))