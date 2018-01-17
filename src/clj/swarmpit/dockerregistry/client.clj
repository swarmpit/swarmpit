(ns swarmpit.dockerregistry.client
  (:require [swarmpit.http :refer :all]))

(def ^:private base-url "https://index.docker.io/v2")

(defn- execute
  [{:keys [method api options]}]
  (let [url (str base-url api)
        options (req-options options)]
    (execute-in-scope {:method        method
                       :url           url
                       :options       options
                       :scope         "Docker registry"
                       :error-handler #(-> % :errors (first) :message)})))

(defn tags
  [token repository-name]
  (-> (execute {:method  :GET
                :api     (str "/" repository-name "/tags/list")
                :options {:headers {"Authorization" (str "Bearer " token)}}})
      :body))

(defn manifest
  [token repository-name repository-tag]
  (-> (execute {:method  :GET
                :api     (str "/" repository-name "/manifests/" repository-tag)
                :options {:headers {"Authorization" (str "Bearer " token)}}})
      :body))

(defn distribution
  [token repository-name repository-tag]
  (-> (execute {:method  :GET
                :api     (str "/" repository-name "/manifests/" repository-tag)
                :options {:headers {"Authorization" (str "Bearer " token)
                                    "Accept"        "application/vnd.docker.distribution.manifest.v2+json"}}})
      :headers
      :docker-content-digest))