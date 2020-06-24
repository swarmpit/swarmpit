(ns swarmpit.log
  (:require [taoensso.encore :as enc]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]))

(defn output-fn
  ([data] (output-fn nil data))
  ([opts data]
   (let [{:keys [no-stacktrace? stacktrace-fonts]} opts
         {:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_
                 timestamp_ ?line]} data
         thread-id (.getId (Thread/currentThread))]
     (str
       (force timestamp_) " "
       (force hostname_) " "
       (str/upper-case (name level)) " "
       "[" (or ?ns-str ?file "?") ":" (or ?line "?") "](" thread-id ") - "
       (force msg_)
       (when-not no-stacktrace?
         (when-let [err ?err]
           (str enc/system-newline (timbre/stacktrace err opts))))))))