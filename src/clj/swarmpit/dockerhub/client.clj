(ns swarmpit.dockerhub.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.token :as token]))

(def ^:private v1-base-url "https://registry.hub.docker.com/v1")
(def ^:private v2-base-url "https://hub.docker.com/v2")

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

(defn- basic-auth
  [user]
  {"Authorization" (token/generate-basic (:username user)
                                         (:password user))})

(defn- jwt-auth
  [token]
  {"Authorization" (str "JWT " token)})

(defn login
  [user]
  (let [api "/users/login"
        url (str v2-base-url api)]
    (post url nil user)))

(defn user-repositories
  [username token]
  (let [api (str "/repositories/" username)
        url (str v2-base-url api)]
    (get url (jwt-auth token) nil)))

(defn repositories
  [query page]
  (let [api "/search/repositories"
        url (str v2-base-url api)
        params {:query     query
                :page      page
                :page_size 20}]
    (get url nil params)))

(defn tags
  [repository user]
  (let [headers (if (some? user)
                  (basic-auth user))
        api (str "/repositories/" repository "/tags")
        url (str v1-base-url api)]
    (get url headers nil)))