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
  [event handler]
  (let [event-data (parse-event event)]
    (event/handle handler event-data)))

(defn- on-open!
  [event handler]
  (print (str "EventSource subscribed for [" handler "]")))

(defn- on-error!
  [event handler error-fx]
  (when (= (.-readyState @es) 2)
    (do
      (print (str "EventSource failed subscribe for [" handler "]. Reconnecting in 5 sec..."))
      (js/setTimeout #(error-fx) 5000))))

(defn close!
  []
  (when (some? @es)
    (.close @es)))

(defn open!
  [route]
  (let [handler (:handler route)
        subscription (dissoc route :data)]
    (handler/get
      (routes/path-for-backend :slt)
      {:on-success (fn [{:keys [slt]}]
                     (let [event-source (js/EventSource. (event-source-url slt subscription))]
                       (.addEventListener event-source "message" (fn [e] (on-message! e handler)))
                       (.addEventListener event-source "open" (fn [e] (on-open! e handler)))
                       (.addEventListener event-source "error" (fn [e] (on-error! e handler #(open! handler))))
                       (reset! es event-source)))})))

(defn subscribe!
  [route]
  (close!)
  (when (contains? event/handlers (:handler route))
    (open! route)))