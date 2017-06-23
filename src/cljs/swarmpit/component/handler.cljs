(ns swarmpit.component.handler
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.storage :as storage]
            [swarmpit.component.progress :as progress]
            [clojure.walk :refer [keywordize-keys]]))

(defn- command
  [params success-fx error-fx]
  {:params        params
   :headers       {"Authorization" (storage/get "token")}
   :handler       (fn [response]
                    (let [resp (keywordize-keys response)]
                      (-> resp success-fx)))
   :error-handler (fn [response]
                    (let [resp (:response (keywordize-keys response))]
                      (-> resp error-fx)))})

(defn- command-progress
  [params headers success-fx error-fx]
  {:format        :json
   :params        params
   :headers       (merge {"Authorization" (storage/get "token")} headers)
   :finally       (progress/mount!)
   :handler       (fn [response]
                    (progress/unmount!)
                    (let [resp (keywordize-keys response)]
                      (-> resp success-fx)))
   :error-handler (fn [response]
                    (progress/unmount!)
                    (let [resp (:response (keywordize-keys response))]
                      (-> resp error-fx)))})

(defn get
  [api success-fx error-fx]
  (ajax/GET api
            (command nil success-fx error-fx)))

(defn delete
  [api success-fx error-fx]
  (ajax/DELETE api
               (command nil success-fx error-fx)))

(defn post
  ([api payload success-fx error-fx] (post api payload {} success-fx error-fx))
  ([api payload headers success-fx error-fx]
   (ajax/POST api
              (command-progress payload headers success-fx error-fx))))

