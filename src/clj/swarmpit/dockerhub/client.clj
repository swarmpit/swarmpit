(ns swarmpit.dockerhub.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]))

(def ^:private base-url "https://hub.docker.com/v2")

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Dockerhub client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-string body true)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Dockerhub error!"
                     {:status status
                      :body   {:error response}})))))))

(defn- get
  [url headers params]
  (let [options {:headers      headers
                 :query-params params}]
    (execute @(http/get url options))))

(defn- post
  [url headers body]
  (let [options {:headers (merge headers {"Content-Type" "application/json"})
                 :body    (generate-string body)}]
    (execute @(http/post url options))))

(defn- jwt-auth
  [token]
  {"Authorization" (str "JWT " token)})

(defn login
  [user]
  (let [api "/users/login"
        url (str base-url api)]
    (post url nil user)))

(defn info
  [user]
  (let [api (str "/users/" (:username user))
        url (str base-url api)]
    (get url nil nil)))

(defn repositories-by-namespace
  [token namespace]
  (let [api (str "/repositories/" namespace)
        url (str base-url api)]
    (get url (jwt-auth token) {:page_size 1000})))

(defn namespaces
  [token]
  (let [api "/repositories/namespaces"
        url (str base-url api)]
    (get url (jwt-auth token) nil)))

(defn repositories
  [query page]
  (let [api "/search/repositories"
        url (str base-url api)
        params {:query     query
                :page      page
                :page_size 20}]
    (get url nil params)))