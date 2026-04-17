(ns swarmpit.docker.engine.cli
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :refer [make-parents delete-file]]
            [cheshire.core :refer [parse-string]]
            [swarmpit.config :refer [config]]))

(def ^:private stack-name-pattern #"^[a-zA-Z0-9][a-zA-Z0-9_-]{0,62}$")

(defn- validate-stack-name!
  [name]
  (when-not (and (string? name) (re-matches stack-name-pattern name))
    (throw
      (ex-info (str "Invalid stack name: " (pr-str name))
               {:status 400
                :type   :docker-cli
                :body   {:error "stack name must match [a-zA-Z0-9][a-zA-Z0-9_-]{0,62}"}}))))

(defn- execute
  "Execute docker command and parse result"
  [cmd]
  (let [result (apply shell/sh cmd)]
    (if (= 0 (:exit result))
      {:result (:out result)}
      (throw
        (let [error (:err result)]
          (ex-info (str "Docker error: " error)
                   {:status 400
                    :type   :docker-cli
                    :body   {:error error}}))))))

(defn- login-cmd
  [username password server]
  ["docker" "login" "--username" username "--password" password (or server "")])

(defn- stack-deploy-cmd
  [name file {:keys [skip-resolve-image]}]
  (cond-> ["docker" "stack" "deploy" "--with-registry-auth" "--compose-file" file]
    skip-resolve-image (conj "--resolve-image" "never")
    true               (conj name)))

(defn- stack-remove-cmd
  [name]
  ["docker" "stack" "rm" name])

(defn- stack-file
  [name]
  (str (config :work-dir) "/" name ".yml"))

(defn login
  [username password server]
  (-> (login-cmd username password server)
      (execute)))

(defn stack-deploy
  [name compose & [{:keys [skip-resolve-image] :as opts}]]
  (validate-stack-name! name)
  (let [file (stack-file name)
        cmd (stack-deploy-cmd name file opts)]
    (try
      (make-parents file)
      (spit file compose)
      (execute cmd)
      (finally
        (delete-file file)))))

(defn stack-remove
  [name]
  (validate-stack-name! name)
  (let [cmd (stack-remove-cmd name)]
    (execute cmd)))
