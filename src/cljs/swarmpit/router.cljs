(ns swarmpit.router
  (:require [bidi.router :as br]
            [cemerick.url :refer [query->map]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.component.state :as state]
            [swarmpit.url :refer [query-string]]
            [swarmpit.routes :as routes]))

(def cursor [:route])

(defonce !router (atom nil))

(defonce !route (atom nil))

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
                                 :params  (merge route-params query-params)} cursor))))

(defn- on-navigate
  [location]
  (set-route location))

(defn not-found!
  [body]
  (if (not (= :not-found (:handler @!route)))
    (on-navigate {:handler      :not-found
                  :route-params {:origin (state/get-value state/route-cursor) :error body}})))

(defn start
  []
  (let [router (br/start-router! routes/frontend {:on-navigate on-navigate})]
    (reset! !router router)))
