(ns swarmpit.event.handler
  (:require [org.httpkit.server :refer [with-channel on-close send!]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.slt :as slt]
            [swarmpit.event.channel :refer [channel-hub broadcast-memo]]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error resp-unauthorized]]))

(defmethod dispatch :events [_]
  (fn [{:keys [query-params] :as request}]
    (let [slt (-> (keywordize-keys query-params) :slt)]
      (if (slt/valid? slt)
        (with-channel request channel
                      (send! channel {:status  200
                                      :headers {"Content-Type"  "text/event-stream"
                                                "Cache-Control" "no-cache"
                                                "Connection"    "keep-alive"}
                                      :body    ":ok\n\n"} false)
                      (swap! channel-hub assoc channel request)
                      (on-close channel (fn [_]
                                          (swap! channel-hub dissoc channel))))
        (resp-unauthorized "Invalid slt")))))

(defmethod dispatch :event-push [_]
  (fn [{:keys [params]}]
    (if (some? params)
      (let [event-message (:Message (keywordize-keys params))]
        (broadcast-memo event-message)
        (resp-accepted (str "Broadcasted to " (count @channel-hub) " clients")))
      (resp-error 400 "No data sent"))))