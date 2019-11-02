(ns swarmpit.event.handler
  (:require [org.httpkit.server :refer [with-channel on-close send!]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [swarmpit.handler :refer [resp-accepted resp-error resp-unauthorized]]
            [swarmpit.event.channel :as channel]
            [swarmpit.event.processor :as processor]
            [swarmpit.event.rules.predicate :refer [stats?]]
            [swarmpit.slt :as slt]))

(defn events
  [{:keys [parameters] :as request}]
  (let [slt (get-in parameters [:query :slt])]
    (if (slt/valid? slt)
      (let [user (slt/user slt)
            request (assoc-in request [:identity] user)]
        (with-channel request channel
                      (send! channel {:status  200
                                      :headers {"Content-Type"                "text/event-stream"
                                                "Access-Control-Allow-Origin" "*"
                                                "Cache-Control"               "no-cache"
                                                "Connection"                  "keep-alive"}
                                      :body    ":ok\n\n"} false)
                      (swap! channel/hub assoc channel request)
                      (on-close channel (fn [_]
                                          (swap! channel/hub dissoc channel)))))
      (resp-unauthorized "Invalid slt"))))

(defn event-push
  [{{:keys [body]} :parameters}]
  (if (some? body)
    (let [event body]
      (processor/process event)
      (if (stats? (:type event))
        (channel/broadcast-statistics)
        (channel/broadcast-memo event))
      (resp-accepted))
    (resp-error 400 "No data sent")))