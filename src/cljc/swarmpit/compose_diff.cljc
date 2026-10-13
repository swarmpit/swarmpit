(ns swarmpit.compose-diff
  (:require [clojure.string :as str]))

(defn restoration
  [current {:keys [b-start b-end lines]}]
  (let [n (count current)
        end {:line (dec n) :ch (count (peek current))}
        position #(if (< % n) {:line % :ch 0} end)]
    {:from (position b-start)
     :to   (position b-end)
     :text (str (when (= b-start n) "\n")
                (str/join "\n" lines)
                (when (< b-end n) "\n"))}))
