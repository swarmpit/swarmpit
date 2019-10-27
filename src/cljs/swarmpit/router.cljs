(ns swarmpit.router
  (:require [reitit.frontend.easy :as rfe]
            [cemerick.url :refer [query->map]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.component.state :as state]
            [swarmpit.url :refer [query-string]]
            [swarmpit.routes :as routes]))

(def cursor [:route])

(defonce !route (atom nil))

(defn set-route
  [location]
  (reset! !route location))

(add-watch !route :watcher
           (fn [key atom old-location new-location]
             (let [query-params (keywordize-keys (query->map (query-string)))
                   route-params (:route-params new-location)
                   handler (:handler new-location)]
               (state/set-value {:handler handler
                                 :params  (merge route-params query-params)} cursor))))

(defn- on-navigate
  [{:keys [data path-params] :as match}]
  (set-route {:handler      (:name data)
              :route-params path-params}))

(defn not-found!
  [body]
  (if (not (= :not-found (:handler @!route)))
    (set-route {:handler      :not-found
                :route-params {:origin (state/get-value state/route-cursor) :error body}})))

(defn start
  []
  (rfe/start!
    routes/frontend-router
    (fn [m] (on-navigate m))
    ;; set to false to enable HistoryAPI
    {:use-fragment true}))


