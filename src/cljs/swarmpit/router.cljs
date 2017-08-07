(ns swarmpit.router
  (:require [bidi.router :as br]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.controller :as controller]))

(defonce location (atom nil))

(defn- on-navigate
  [location]
  (if (nil? (storage/get "token"))
    (do (controller/dispatch {:handler :login})
        (dispatch!
          (routes/path-for-frontend :login)))
    ;; Render to service list by default as we don't have any index page right now
    (if (= :index (:handler location))
      (controller/dispatch {:handler :service-list})
      (controller/dispatch location))))

(defn start
  []
  (let [router (br/start-router! routes/frontend {:on-navigate on-navigate})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router @location))))


