(ns swarmpit.component.handler
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.storage :as storage]
            [swarmpit.component.progress :as progress]
            [clojure.walk :refer [keywordize-keys]]))

(defn- command
  [request]
  {:params        (:params request)
   :headers       {"Authorization" (storage/get "token")}
   :finally       (:on-call request)
   :handler       (fn [response]
                    (let [resp (keywordize-keys response)
                          resp-fx (:on-success request)]
                      (-> resp resp-fx)))
   :error-handler (fn [response]
                    (let [resp (:response (keywordize-keys response))
                          resp-fx (or (:on-error request) #())]
                      (-> resp resp-fx)))})

(defn- command-progress
  [request]
  {:format        :json
   :params        (:params request)
   :headers       (merge {"Authorization" (storage/get "token")} (:headers request))
   :finally       (progress/mount!)
   :handler       (fn [response]
                    (progress/unmount!)
                    (let [resp (keywordize-keys response)
                          resp-fx (:on-success request)]
                      (-> resp resp-fx)))
   :error-handler (fn [response]
                    (progress/unmount!)
                    (let [resp (:response (keywordize-keys response))
                          resp-fx (or (:on-error request) #())]
                      (-> resp resp-fx)))})

(defn get
  [api request]
  (ajax/GET api (command request)))

(defn delete
  [api request]
  (ajax/DELETE api (command request)))

(defn post
  [api request]
  (ajax/POST api (command-progress request)))

