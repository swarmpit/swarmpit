(ns swarmpit.event.source
  (:require [swarmpit.routes :as routes]
            [swarmpit.component.handler :as handler]
            [swarmpit.event.handler :as event]
            [goog.crypt.base64 :as b64]
            [clojure.walk :refer [keywordize-keys]]))

(def es (atom nil))

(defn- parse-event
  [event]
  (-> (.-data event)
      (js/JSON.parse)
      (js->clj)
      (keywordize-keys)))

(defn- event-source-url
  [token subscription]
  (routes/path-for-backend :events {} {:slt          token
                                       :subscription (b64/encodeString subscription)}))

(defn- on-message!
  [event route]
  (let [event-data (parse-event event)]
    (event/handle (:handler route) event-data)))

(defn- on-open!
  [event route]
  (print (str "EventSource subscribed for " route)))

(defn- on-error!
  [event route error-fx]
  (when (= (.-readyState @es) 2)
    (do
      (print (str "EventSource failed subscribe for " route ". Reconnecting in 5 sec..."))
      (js/setTimeout #(error-fx) 5000))))

(defn close!
  []
  (when (some? @es)
    (.close @es)))

(defn open!
  [route]
  (handler/get
    (routes/path-for-backend :slt)
    {:on-success (fn [{:keys [slt]}]
                   (let [event-source (js/EventSource. (event-source-url slt route))]
                     (.addEventListener event-source "message" (fn [e] (on-message! e route)))
                     (.addEventListener event-source "open" (fn [e] (on-open! e route)))
                     (.addEventListener event-source "error" (fn [e] (on-error! e route #(open! route))))
                     (reset! es event-source)))}))