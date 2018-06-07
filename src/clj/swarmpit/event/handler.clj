(ns swarmpit.event.handler
  (:require [org.httpkit.server :refer [with-channel on-close send!]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error resp-unauthorized]]
            [swarmpit.event.channel :as channel]
            [swarmpit.event.processor :as processor]
            [swarmpit.event.rules.predicate :refer [stats?]]
            [swarmpit.slt :as slt]))

(defmethod dispatch :events [_]
  (fn [{:keys [query-params] :as request}]
    (let [slt (-> (keywordize-keys query-params) :slt)]
      (if (slt/valid? slt)
        (with-channel request channel
                      (send! channel {:status  200
                                      :headers {"Content-Type"                "text/event-stream"
                                                "Access-Control-Allow-Origin" "*"
                                                "Cache-Control"               "no-cache"
                                                "Connection"                  "keep-alive"}
                                      :body    ":ok\n\n"} false)
                      (swap! channel/hub assoc channel request)
                      (on-close channel (fn [_]
                                          (swap! channel/hub dissoc channel))))
        (resp-unauthorized "Invalid slt")))))

(defmethod dispatch :event-push [_]
  (fn [{:keys [params]}]
    (if (some? params)
      (let [event (keywordize-keys params)]
        (processor/process event)
        (if (stats? (:type event))
          (channel/broadcast-statistics)
          (channel/broadcast-memo event))
        (resp-accepted))
      (resp-error 400 "No data sent"))))