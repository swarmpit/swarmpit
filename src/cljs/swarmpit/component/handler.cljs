(ns swarmpit.component.handler
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str :refer [capitalize]]))

(defn- resource
  [handler]
  (first (str/split (name handler) "-")))

(defn- execute
  ([success-fx error-fx]
   {:headers       {"Authorization" (storage/get "token")}
    :handler       (fn [response]
                     (let [resp (keywordize-keys response)]
                       (-> resp success-fx)))
    :error-handler (fn [response]
                     (let [resp (keywordize-keys response)]
                       (-> resp error-fx)))}))

(defn get
  ([api success-fx]
   (get api success-fx (fn [{:keys [status]}]
                         (if (= status 401)
                           (state/set-value {:handler :unauthorized} [:location])
                           (state/set-value {:handler :error} [:location])))))
  ([api success-fx error-fx]
   (ajax/GET api
             (execute success-fx error-fx))))