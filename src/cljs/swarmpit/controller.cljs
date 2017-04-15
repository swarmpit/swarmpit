(ns swarmpit.controller
  (:require [bidi.router :as br]
            [clojure.walk :as walk]
            [ajax.core :refer [GET POST]]
            [swarmpit.component.service.create :as screate]
            [swarmpit.component.service.edit :as sedit]
            [swarmpit.component.service.info :as sinfo]
            [swarmpit.component.service.list :as slist]))

(defmulti dispatch (fn [location] (:handler location)))

(def location (atom nil))

(def handler ["" {"/"         :index
                  "/services" {""                :service-list
                               "/create"         :service-create
                               ["/" :id]         :service-info
                               ["/" :id "/edit"] :service-edit}}])

(defn start
  []
  (let [router (br/start-router! handler
                                 {:on-navigate
                                  (fn [loc]
                                    (dispatch loc)
                                    (reset! location loc))})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router @location))))

(defmethod dispatch :index
  [_]
  (print "index"))

(defmethod dispatch :service-list
  [_]
  (GET "/services"
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (slist/mount! res)))}))

(defmethod dispatch :service-info
  [{:keys [route-params]}]
  (GET (str "/services/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (sinfo/mount! res)))}))

(defmethod dispatch :service-create
  [_]
  (print "create")
  (screate/mount!))

(defmethod dispatch :service-edit
  [{:keys [route-params]}]
  (print "edit")
  (GET (str "/services/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (sedit/mount! res)))}))

(defmethod dispatch nil
  [_]
  (print "not-found"))




