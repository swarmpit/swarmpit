(ns swarmpit.docker.client
  (:refer-clojure :exclude [get])
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [cheshire.core :refer [parse-string generate-string]]))

(def ^:private api-version "v1.24")
(def ^:private base-cmd ["curl" "--unix-socket" "/var/run/docker.sock" "-w" "%{http_code}"])

(defn- map-headers
  "Map request headers map to curl vector cmd representation"
  [headers]
  (->> headers
       (map #(str (name (key %)) ": " (val %)))
       (map #(into ["-H" %]))
       (flatten)
       (into [])))

(defn- map-uri
  "Map request uri to curl vector cmd representation"
  [method uri]
  ["-X" method (str "http:/" api-version uri)])

(defn- map-payload
  "Map request payload to curl vector cmd representation"
  [json]
  (if (nil? json)
    []
    ["-d" (generate-string json {:pretty true})]))

(defn- command
  "Build docker command"
  [method uri headers payload]
  (let [pheaders (map-headers headers)
        pcommand (map-uri method uri)
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
  [method uri headers payload]
  (let [cmd (command method uri headers payload)
        cmd-result (apply shell/sh cmd)]
    (if (= 0 (:exit cmd-result))
      (let [response (string/split (:out cmd-result) #"\n")
            response-body (parse-body response)
            response-code (parse-http-code response)]
        (if (> 400 response-code)
          response-body
          (throw (ex-info "Docker engine error!"
                          (assoc response-body :code response-code)))))
      (throw (ex-info "Docker client failure!"
                      (parse-string (:err cmd-result) true))))))

(defn get
  ([uri] (execute "GET" uri nil nil))
  ([uri headers] (execute "GET" uri headers nil)))

(defn post
  ([uri payload] (execute "POST" uri nil payload))
  ([uri headers payload] (execute "POST" uri headers payload)))

(defn put
  ([uri payload] (execute "PUT" uri nil payload))
  ([uri headers payload] (execute "PUT" uri headers payload)))

(defn delete
  ([uri] (execute "DELETE" uri nil nil))
  ([uri headers] (execute "DELETE" uri headers nil)))