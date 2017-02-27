(ns swarmpit.controller
  (:require [bidi.router :as br]
            [ajax.core :refer [GET POST]]
            [swarmpit.component.service.form-create :as form]))

(defmulti dispatch identity)

(def location (atom nil))

(def handler ["" {"/"         :index
                  "/services" :services}])

(defn start
  []
  (let [router (br/start-router! handler
                                 {:on-navigate
                                  (fn [loc] (do (dispatch (:handler loc))
                                                (reset! location loc)))})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router {:handler route}))))

(defmethod dispatch :index [_]
  (print "index"))

(defmethod dispatch :services [_]
  (GET "/services" {:handler (fn [response]
                               (do (print response)
                                   (form/mount!)))}))

(defmethod dispatch nil [_]
  (print "not-found"))




