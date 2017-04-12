(ns swarmpit.docker.client
  (:refer-clojure :exclude [get])
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [cheshire.core :refer [parse-string]]))

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
    ["-d" json]))

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

(defn- parse-payload
  [response]
  (if (= (count response) 1)
    nil
    (parse-string (first response) true)))

(defn- parse-http-code
  [response]
  (if (= (count response) 1)
    (Integer. (first response))
    (Integer. (second response))))

(defn- execute
  "Execute docker command and parse result"
  [method uri headers payload]
  (let [cmd (command method uri headers payload)
        result (apply shell/sh cmd)]
    (if (= 0 (:exit result))
      (let [response (string/split (:out result) #"\n")
            payload (parse-payload response)
            http-code (parse-http-code response)]
        (if (> 400 http-code)
          payload
          (throw (ex-info "Docker engine error!"
                          (assoc payload :code http-code)))))
      (throw (ex-info "Docker client failure!"
                      (parse-string (:err result) true))))))

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