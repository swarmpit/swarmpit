(ns swarmpit.router
  (:require [bidi.router :as br]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [cemerick.url :refer [query->map]]
            [swarmpit.url :refer [dispatch! query-string]]
            [clojure.walk :refer [keywordize-keys]]))

(def cursor [:route])

(defonce location (atom nil))

(defn navigate!
  ([location]
   (navigate! location false))
  ([{:keys [handler route-params]} dispatch?]
   (let [query-params (keywordize-keys (query->map (query-string)))]
     (state/set-value {:handler handler
                       :params  (merge route-params query-params)} cursor)
     (when dispatch?
       (dispatch! (routes/path-for-frontend handler))))))

(defn- on-navigate
  [location]
  (let [token (storage/get "token")]
    (if (nil? token)
      (navigate! {:handler :login} true)
      ;; Render to service list by default as we don't have any index page right now
      (if (= :index (:handler location))
        (navigate! {:handler :service-list})
        (navigate! location)))))

(defn start
  []
  (let [router (br/start-router! routes/frontend {:on-navigate on-navigate})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router @location))))