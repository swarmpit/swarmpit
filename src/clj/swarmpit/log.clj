(ns swarmpit.log
  (:require [cheshire.core :refer [generate-string]]
            [swarmpit.utils :refer [update-in-if-present]]
            [taoensso.encore :as enc]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]))

(def sensitive-tokens [:password :secret :Authorization])

(defn hide-sensitive-data [fragment-map]
  (update-in-if-present
    fragment-map
    sensitive-tokens
    (fn [a]
      (cond
        (str/starts-with? a "Basic") (str "Basic *****")
        (str/starts-with? a "Bearer") (str "Bearer *****")
        (str/starts-with? a "JWT") (str "JWT *****")
        :else "*****"))))

(defn pretty-print [fragment-map]
  (generate-string (hide-sensitive-data fragment-map) {:pretty true}))

(defn pretty-print-ex [fragment-map]
  (-> fragment-map
      (select-keys [:headers :status :body :reason-phrase :type])
      (pretty-print)))

(defn output-fn
  ([data] (output-fn nil data))
  ([opts data]
   (let [{:keys [no-stacktrace? stacktrace-fonts]} opts
         {:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_
                 timestamp_ ?line]} data
         thread-id (.getId (Thread/currentThread))]
     (str
       (force timestamp_) " "
       (str/upper-case (name level)) " "
       "[" (or ?ns-str ?file "?") ":" (or ?line "?") "](" thread-id ") - "
       (force msg_)
       (when-not no-stacktrace?
         (when-let [err ?err]
           (str enc/system-newline (timbre/stacktrace err opts))))))))