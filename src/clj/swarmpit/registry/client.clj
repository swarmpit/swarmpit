(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [swarmpit.token :as token]))

(defn- build-url
  [registry api]
  (str (:scheme registry) "://"
       (:url registry) "/"
       (:version registry) api))

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Registry client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-string body true)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Registry error!"
                     {:status status
                      :body   {:error (:errors response)}})))))))

(defn- headers
  [registry]
  (let [headers {}]
    (if (:isPrivate registry)
      (assoc headers "Authorization" (token/generate-basic (:user registry)
                                                           (:password registry)))
      headers)))

(defn- get
  [registry api headers params]
  (let [url (build-url registry api)
        options {:timeout      5000
                 :headers      headers
                 :query-params params}]
    (execute @(http/get url options))))

(defn v1-repositories
  [registry query page]
  (let [params {:q    query
                :page page
                :n    25}
        headers (headers registry)]
    (get registry "/search" headers params)))

(defn v2-repositories
  [registry]
  (let [headers (headers registry)]
    (->> (get registry "/_catalog" headers nil)
         :repositories)))

(defn v1-info
  [registry]
  (let [headers (headers registry)]
    (get registry "/_ping" headers nil)))

(defn v2-info
  [registry]
  (let [headers (headers registry)]
    (get registry "/" headers {})))

(defn v1-tags
  [registry repository-name]
  (let [headers (headers registry)]
    (get registry (str "/repositories/" repository-name "/tags") headers {})))

(defn v2-tags
  [registry repository-name]
  (let [headers (headers registry)]
    (get registry (str "/" repository-name "/tags/list") headers {})))
