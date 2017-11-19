(ns swarmpit.handler-events
  (:require [org.httpkit.server :refer [run-server with-channel on-close send! close]]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [generate-string]]
            [immutant.scheduling :refer :all]
            [swarmpit.slt :as slt]
            [swarmpit.handler :refer [dispatch resp-accepted resp-error resp-unauthorized]]))

(def channel-hub (atom {}))

(defn broadcast [data]
  (let [message (str "data: " (generate-string data) "\n\n")]
    (doseq [channel (keys @channel-hub)]
      (send! channel message false))))

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
      (do
        (broadcast params)
        (resp-accepted (str "Broadcasted to " (count @channel-hub) " clients")))
      (resp-error 400 "No data send"))))