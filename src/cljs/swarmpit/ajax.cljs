(ns swarmpit.ajax
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [swarmpit.router :as router]
            [swarmpit.routes]
            [swarmpit.xhrio :as xhrio]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [clojure.walk :refer [keywordize-keys]]))

(defn- login-redirect
  []
  "Redirect to login if authentication failed and store redirect location."
  (let [location js/window.location.hash]
    (when (not= "#/login" location)
      (state/set-value location [:redirect-location]))
    (router/set-route {:handler :login})))

(defn- command-state
  "Update given form state if form origin."
  [request form-id progress?]
  (let [state-cursor (:state request)]
    (when (and (some? state-cursor)
               (state/form-origin? form-id))
      (state/update-value state-cursor progress? state/form-state-cursor))))

(defn- command-error
  [{:keys [body headers]} status]
  (cond
    (and (= 401 status)
         (= "swarmpit" (:x-backend-server headers))) (login-redirect)
    (and (= 403 status)
         (= "swarmpit" (:x-backend-server headers))) (router/set-route {:handler :unauthorized})
    (= 404 status) (router/not-found! body)
    (= 500 status) (message/error (str (or (:cause body) "Server request failed")))
    (= 502 status) (message/error "Server request failed. Bad Gateway")
    (= 503 status) (message/error "Server request failed. Service Unavailable")
    (= 504 status) (message/error "Server request failed. Gateway Timeout")
    :else (message/error (str (or (:error body) body "Server request failed")))))

(defn- command
  "Customized ajax command:

   params :- req body in case of POST/PUT
   headers :- req headers
   state :- request processing form state cursor
   on-success :- on success handler
   on-error :- on error handler, if missing default error handling used. Check command-error.

   Example usage:

   {:params     {:test 123}
    :headers    {:header true}
    :state      [:processing?]
    :on-success (fn [{:keys [response origin?]}]
                   (print response))
    :on-error   (fn [{:keys [response]}]
                   (print response))}"
  [request]
  (let [form-id (state/form-id)]
    {:response-format {:read        identity
                       :description "raw"}
     :params          (:params request)
     :headers         (merge {"Authorization" (storage/get "token")} (:headers request))
     :finally         (command-state request form-id true)
     :handler         (fn [xhrio]
                        (command-state request form-id false)
                        (let [resp-body (:body (xhrio/response xhrio))
                              resp-fx (:on-success request)]
                          (resp-fx {:response resp-body
                                    :origin?  (state/form-origin? form-id)})))
     :error-handler   (fn [response]
                        (command-state request form-id false)
                        (let [response (keywordize-keys response)
                              resp (xhrio/response (:response response))
                              resp-status (:status response)
                              resp-fx (or (:on-error request)
                                          #(command-error resp resp-status))]
                          (resp-fx {:response (:body resp)
                                    :origin?  (state/form-origin? form-id)})))}))

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

