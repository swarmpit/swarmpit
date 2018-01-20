(ns swarmpit.component.handler
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.router :as router]
            [swarmpit.xhrio :as xhrio]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [clojure.walk :refer [keywordize-keys]]))

(defn- command-state
  [request loading?]
  (when-let [state (:state request)]
    (reset! state loading?)))

(defn- command-error
  [{:keys [body headers]} status]
  (cond
    (= 400 status) (str (:error body))
    (and (= 401 status)
         (= "swarmpit" (:x-backend-server headers))) (router/set-location {:handler :login})
    (and (= 403 status)
         (= "swarmpit" (:x-backend-server headers))) (router/set-location {:handler :unauthorized})
    (= 404 status) (router/set-route {:handler :not-found})
    (= 500 status) (message/error (str (or (:cause body) "Server request failed")))
    :else (message/error body)))

(defn- command
  [request]
  {:response-format {:read        identity
                     :description "raw"}
   :params          (:params request)
   :headers         {"Authorization" (storage/get "token")}
   :finally         (do
                      (command-state request true)
                      (:on-call request))
   :handler         (fn [xhrio]
                      (command-state request false)
                      (let [resp-body (:body (xhrio/response xhrio))
                            resp-fx (:on-success request)]
                        (-> resp-body resp-fx)))
   :error-handler   (fn [response]
                      (command-state request false)
                      (let [response (keywordize-keys response)
                            resp (xhrio/response (:response response))
                            resp-status (:status response)
                            resp-fx (or (:on-error request)
                                        #(command-error resp resp-status))]
                        (-> (:body resp) resp-fx)))})

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

