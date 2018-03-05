(ns swarmpit.ajax
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.router :as router]
            [swarmpit.xhrio :as xhrio]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [clojure.walk :refer [keywordize-keys]]))

(defn- command-state
  [request progress?]
  (when-let [progress-cursor (:progress request)]
    (state/update-value progress-cursor progress? state/form-state-cursor)))

(defn- command-error
  [{:keys [body headers]} status]
  (cond
    (and (= 401 status)
         (= "swarmpit" (:x-backend-server headers))) (router/set-location {:handler :login})
    (and (= 403 status)
         (= "swarmpit" (:x-backend-server headers))) (router/set-location {:handler :unauthorized})
    (= 404 status) (router/set-route {:handler :not-found})
    (= 500 status) (message/error (str (or (:cause body) "Server request failed")))
    :else (message/error body)))

(defn- command
  "Customized ajax command:

   params :- req body in case of POST/PUT
   headers :- req headers
   progress :- cursor of form progress
   on-success :- on success handler
   on-error :- on error handler, if missing default error handling used. Check command-error.

   Example usage:

   {:params     {:test 123}
    :headers    {:header true}
    :progress   [:processing?]
    :on-success (fn [response]
                   (print response))
    :on-error   (fn [response]
                   (print response))}"
  [request]
  {:response-format {:read        identity
                     :description "raw"}
   :params          (:params request)
   :headers         (merge {"Authorization" (storage/get "token")} (:headers request))
   :finally         (command-state request true)
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

(defn get
  [api request]
  (ajax/GET api (command request)))

(defn delete
  [api request]
  (ajax/DELETE api (command request)))

(defn post
  [api request]
  (ajax/POST api (merge (command request)
                        {:format :json})))

