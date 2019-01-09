(ns swarmpit.router
  (:require [bidi.bidi :as bidi]
            [cemerick.url :refer [query->map]]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [pushy.core :as pushy]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]))

(defonce !route (atom {}))

(defn- query-string []
  (when-not (string/blank? js/location.search)
    (.substring js/location.search 1)))

(add-watch !route :watcher
 (fn [key atom old-location new-location]
   (let [query-params (keywordize-keys (query->map (query-string)))
         route-params (:route-params new-location)
         handler (:handler new-location)]
     (state/set-value {:handler handler
                       :params  (merge route-params query-params)} state/route-cursor))))

(defn set-page! [match]
  (if (= :index (:handler match))
    (reset! !route {:handler :stack-list})
    (reset! !route match)))

(def history
  (pushy/pushy set-page! (partial bidi/match-route routes/frontend)))

(defn set-location
  [location]
  (pushy/set-token! history location))

(defn set-route
  [{:keys [handler params query]}]
  (set-location (routes/path-for-frontend handler params query)))

(defn start
  []
  (pushy/start! history))
