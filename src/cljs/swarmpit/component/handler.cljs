(ns swarmpit.component.handler
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.router :as router]
            [swarmpit.storage :as storage]
            [swarmpit.component.progress :as progress]
            [clojure.walk :refer [keywordize-keys]]))

(defn- command-state
  [request loading?]
  (when-let [state (:state request)]
    (reset! state loading?)))

(defn- command-error
  [resp-status]
  (case resp-status
    401 (router/navigate! {:handler :login} true)
    403 (router/navigate! {:handler :unauthorized} true)
    404 (router/navigate! {:handler :not-found} true)
    (router/navigate! {:handler :error} true)))

(defn- command
  [request]
  {:params        (:params request)
   :headers       {"Authorization" (storage/get "token")}
   :finally       (do
                    (command-state request true)
                    (:on-call request))
   :handler       (fn [response]
                    (command-state request false)
                    (let [resp (keywordize-keys response)
                          resp-fx (:on-success request)]
                      (-> resp resp-fx)))
   :error-handler (fn [response]
                    (command-state request false)
                    (let [resp (keywordize-keys response)
                          resp-body (:response resp)
                          resp-status (:status resp)
                          resp-fx (or (:on-error request)
                                      #(command-error resp-status))]
                      (-> resp-body resp-fx)))})

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

