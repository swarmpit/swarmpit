(ns swarmpit.docker.registry.client
  (:require [swarmpit.http :refer :all]))

(def ^:private base-url "https://index.docker.io/v2")

(defn- execute
  [{:keys [method api options]}]
  (execute-in-scope {:method        method
                     :url           (str base-url api)
                     :options       options
                     :scope         "Docker registry"
                     :error-handler #(-> % :errors (first) :message)}))

(defn tags
  [token repository-name]
  (-> (execute {:method  :GET
                :api     (str "/" repository-name "/tags/list")
                :options {:headers {:Authorization (str "Bearer " token)}}})
      :body))

(defn- request-manifest
  [token repository-name repository-tag method type]
  (let [response (execute {:method  method
                           :api     (str "/" repository-name "/manifests/" repository-tag)
                           :options {:headers {:Authorization (str "Bearer " token)
                                               :Accept        type}}})
        response-type (get-in response [:headers :content-type])]
    (when (= type response-type) response)))

(defn manifest
  [token repository-name repository-tag]
  (:body (request-manifest token repository-name repository-tag :GET
                           "application/vnd.docker.distribution.manifest.v1+prettyjws")))

(defn digest
  [token repository-name repository-tag]
  (-> (or (request-manifest token repository-name repository-tag :HEAD
                            "application/vnd.docker.distribution.manifest.list.v2+json")
          (request-manifest token repository-name repository-tag :HEAD
                            "application/vnd.docker.distribution.manifest.v2+json"))
      :headers
      :docker-content-digest))