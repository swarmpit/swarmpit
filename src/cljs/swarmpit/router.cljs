(ns swarmpit.router
  (:require [bidi.router :as br]
            [swarmpit.routes :as routes]
            [swarmpit.component.state :as state]
            [cemerick.url :refer [query->map]]
            [swarmpit.url :refer [dispatch! query-string]]
            [clojure.walk :refer [keywordize-keys]]))

(defonce !router (atom nil))

(defonce !route (atom nil))

(defn replace-location
  [location]
  (br/replace-location! @!router location))

(defn set-location
  [location]
  (br/set-location! @!router location))

(defn set-route
  [location]
  (reset! !route location))

(add-watch !route :watcher
           (fn [key atom old-location new-location]
             (let [query-params (keywordize-keys (query->map (query-string)))
                   route-params (:route-params new-location)
                   handler (:handler new-location)]
               (state/set-value {:handler handler
                                 :params  (merge route-params query-params)} state/route-cursor))))

(defn- on-navigate
  [location]
  ;; Render to service list by default as we don't have any index page right now
  (if (= :index (:handler location))
    (set-route {:handler :stack-list})
    (set-route location)))

(defn start
  []
  (let [router (br/start-router! routes/frontend {:on-navigate on-navigate})]
    (reset! !router router)))