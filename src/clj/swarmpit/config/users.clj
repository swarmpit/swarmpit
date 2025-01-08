(ns swarmpit.config.users
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [swarmpit.api :as api]
            [swarmpit.docker.secret :as secret]
            [clojure.string :as str]
            [environ.core :refer [env]]))

(def ^:private config-path "/run/configs/users.yaml")
(def ^:private secrets-path "/run/secrets")

(defn- read-secret
  "Read a Docker secret from the secrets directory"
  [secret-name]
  (try
    (-> (str secrets-path "/" secret-name)
        (slurp)
        (str/trim))
    (catch Exception _
      nil)))

(defn- get-password
  "Get password from the various possible sources"
  [{:keys [password password_hash password_env password_secret]}]
  (cond
    password_hash password_hash
    password password
    password_env (env (keyword (str/lower-case password_env)))
    password_secret (read-secret password_secret)
    :else nil))

(defn- create-user
  "Create a user if it doesn't exist"
  [{:keys [username email role] :as user-config}]
  (let [password (get-password user-config)]
    (when (and username password)
      (let [user {:username username
                  :password password
                  :email email
                  :role (or role "viewer")}]
        (when (not (api/user-exist? user))
          (api/create-user user))))))

(defn init!
  "Initialize users from configuration file if it exists"
  []
  (try
    (when (.exists (io/file config-path))
      (let [config (-> config-path
                      (slurp)
                      (yaml/parse-string))
            users (:users config)]
        (doseq [user users]
          (create-user user))))
    (catch Exception e
      (println "Error initializing users from config:" (.getMessage e)))))

(defn parse-user-config
  [user]
  (let [username (get user :username)
        password (get user :password)
        password-secret (get user :password_secret)
        password-env (get user :password_env)
        role (get user :role "viewer")]
    {:username username
     :password (cond
                password password
                password-secret (secret/get password-secret)
                password-env (env (keyword (str/lower-case password-env)))
                :else nil)
     :role role})) 