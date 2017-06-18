(ns swarmpit.docker.client
  (:refer-clojure :exclude [get])
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [ring.util.codec :refer [form-encode]]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.token :as token]))

(def ^:private api-version "v1.27")
(def ^:private base-cmd ["curl" "--unix-socket" "/var/run/docker.sock" "-w" "%{http_code}"])

(defn- map-headers
  "Map request `headers` map to curl vector cmd representation"
  [headers]
  (->> headers
       (map #(str (name (key %)) ": " (val %)))
       (map #(into ["-H" %]))
       (flatten)
       (into [])))

(defn- map-params
  "Map request query `params` to curl vector cmd representation"
  [params]
  (if (some? params)
    (str "?" (form-encode params))
    ""))

(defn- map-uri
  "Map request `method`, `uri` & query `params` to curl vector cmd representation"
  [method uri params]
  ["-X" method (str "http:/" api-version uri (map-params params))])

(defn- map-payload
  "Map request `payload` to curl vector cmd representation"
  [payload]
  (if (nil? payload)
    []
    ["-d" (generate-string payload {:pretty true})]))

(defn- command
  "Build docker command"
  [method uri params headers payload]
  (let [pheaders (map-headers headers)
        pcommand (map-uri method uri params)
        ppayload (map-payload payload)]
    (-> base-cmd
        (into pheaders)
        (into ppayload)
        (into pcommand))))

(defn- parse-body
  "Parse docker engine response body"
  [response]
  (if (= (count response) 1)
    ""
    (parse-string (first response) true)))

(defn- parse-http-code
  "Parse docker engine response http code"
  [response]
  (Integer. (if (= (count response) 1)
              (first response)
              (second response))))

(defn- execute
  "Execute docker command and parse result"
  [method uri params headers payload]
  (let [cmd (command method uri params headers payload)
        cmd-result (apply shell/sh cmd)]
    (if (= 0 (:exit cmd-result))
      (let [response (string/split (:out cmd-result) #"\n")
            response-body (parse-body response)
            response-code (parse-http-code response)]
        (if (> 400 response-code)
          response-body
          (throw (ex-info "Docker engine error!"
                          {:status response-code
                           :body   {:error response-body}}))))
      (throw (ex-info "Docker client failure!"
                      {:status 500
                       :body   {:error (parse-string (:err cmd-result) true)}})))))

(defn- get
  ([uri] (execute "GET" uri nil nil nil))
  ([uri params] (execute "GET" uri params nil nil))
  ([uri params headers] (execute "GET" uri params headers nil)))

(defn- post
  ([uri payload] (post uri nil nil payload))
  ([uri params payload] (post uri params nil payload))
  ([uri params headers payload]
   (execute "POST" uri params (merge headers {:Content-Type "application/json"}) payload)))

(defn- put
  ([uri payload] (put uri nil nil payload))
  ([uri headers payload] (put uri nil headers payload))
  ([uri params headers payload]
   (execute "PUT" uri params (merge headers {:Content-Type "application/json"}) payload)))

(defn- delete
  ([uri] (execute "DELETE" uri nil nil nil))
  ([uri headers] (execute "DELETE" uri nil headers nil)))

(defn- registry-token
  [auth]
  (token/generate-base64 (generate-string auth)))

;; Service

(defn services
  []
  (get "/services"))

(defn service
  [id]
  (-> (str "/services/" id)
      (get)))

(defn delete-service
  [id]
  (-> (str "/services/" id)
      (delete)))

(defn create-service
  ([service]
   (post "/services/create" {} nil service))
  ([auth-config service]
   (let [headers {:X-Registry-Auth (registry-token auth-config)}]
     (post "/services/create" {} headers service))))

(defn update-service
  [id version service]
  (let [uri (str "/services/" id "/update")]
    (post uri {:version version} service)))

;; Task

(defn tasks
  []
  (get "/tasks"))

(defn task
  [id]
  (-> (str "/tasks/" id)
      (get)))

;; Network

(defn networks
  []
  (get "/networks"))

(defn network
  [id]
  (-> (str "/networks/" id)
      (get)))

(defn delete-network
  [id]
  (-> (str "/networks/" id)
      (delete)))

(defn create-network
  [network]
  (post "/networks/create" network))

;; Volume

(defn volumes
  []
  (get "/volumes"))

(defn volume
  [name]
  (-> (str "/volumes/" name)
      (get)))

(defn delete-volume
  [name]
  (-> (str "/volumes/" name)
      (delete)))

(defn create-volume
  [volume]
  (post "/volumes/create" volume))

;; Secret

(defn secrets
  []
  (get "/secrets"))

(defn secret
  [id]
  (-> (str "/secrets/" id)
      (get)))

(defn delete-secret
  [id]
  (-> (str "/secrets/" id)
      (delete)))

(defn create-secret
  [secret]
  (post "/secrets/create" secret))

(defn update-secret
  [id version secret]
  (let [uri (str "/secrets/" id "/update")]
    (post uri {:version version} secret)))

;; Node

(defn nodes
  []
  (get "/nodes"))

(defn node
  [id]
  (-> (str "/nodes/" id)
      (get)))
