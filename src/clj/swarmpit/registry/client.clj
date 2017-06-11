(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.token :as token]))

(defn- build-url
  [registry api]
  (str (:scheme registry) "://"
       (:url registry) "/v2" api))

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
                      :body   {:error response}})))))))

(defn- get
  [registry api headers params]
  (let [url (build-url registry api)
        options {:timeout      5000
                 :headers      headers
                 :query-params params}]
    (execute @(http/get url options))))

(defn- post
  [registry api headers body]
  (let [url (build-url registry api)
        options {:headers (merge headers {"Content-Type" "application/json"})
                 :body    (generate-string body)}]
    (execute @(http/post url options))))

(defn- basic-auth
  [registry]
  {:Authorization (token/generate-basic (:username registry)
                                        (:password registry))})

(defn- jwt-auth
  [token]
  {:Authorization (str "JWT " token)})

;; Dockerhub

(defn dockerhub-login
  [registry]
  (let [body (select-keys registry [:username :password])]
    (post registry "/users/login" {} body)))

(defn dockerhub-user-repo
  [registry token]
  (let [api (str "/repositories/" (:username registry))]
    (get registry api {} (jwt-auth token))))

(defn dockerhub-repositories
  [registry query page]
  (let [params {:query     query
                :page      page
                :page_size 20}]
    (get registry "/search/repositories" {} params)))

(defn dockerhub-tags
  [registry repository-name]
  (get registry (str "/repositories/" repository-name "/tags") {} {}))

;; Classic v2 registry

(defn- headers
  [registry]
  (if (:withAuth registry)
    (basic-auth registry)
    {}))

(defn repositories
  [registry]
  (let [headers (headers registry)]
    (->> (get registry "/_catalog" headers nil)
         :repositories)))

(defn info
  [registry]
  (let [headers (headers registry)]
    (get registry "/" headers {})))

(defn tags
  [registry repository-name]
  (let [headers (headers registry)
        api (str "/" repository-name "/tags/list")]
    (get registry api headers {})))
