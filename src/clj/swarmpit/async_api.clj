(ns swarmpit.async-api
  (:require [clojure.core.async :refer [go]]
            [swarmpit.api :as api]
            [swarmpit.event.channel :as channel]
            [swarmpit.docker.engine.cli :as dcli]
            [swarmpit.couchdb.client :as cc]
            [clojure.tools.logging :as log]))

;; Stack Async API

(defn create-stack
  "Create stack asynchronously and notify owner abount progress"
  [owner {:keys [name spec] :as stackfile}]
  (let [stackfile-origin (cc/stackfile name)]
    (go
      (try
        (api/stack-login owner spec)
        (dcli/stack-deploy name (:compose spec))
        (if (some? stackfile-origin)
          (cc/update-stackfile stackfile-origin stackfile)
          (cc/create-stackfile stackfile))
        (channel/broadcast-info owner (str "Stack " name " deployment finished."))
        (catch Exception ex
          (channel/broadcast-error owner (str "Stack " name " deployment failed.") ex))))))

(defn update-stack
  "Update stack asynchronously and notify owner abount progress"
  [owner {:keys [name spec]}]
  (let [stackfile-origin (cc/stackfile name)]
    (go
      (try
        (api/stack-login owner spec)
        (dcli/stack-deploy name (:compose spec))
        (cc/update-stackfile stackfile-origin {:spec         spec
                                               :previousSpec (:spec stackfile-origin)})
        (channel/broadcast-info owner (str "Stack " name " update finished."))
        (catch Exception ex
          (channel/broadcast-error owner (str "Stack " name " update failed.") ex))))))

(defn redeploy-stack
  "Redeploy stack asynchronously and notify owner abount progress"
  [owner name]
  (let [{:keys [name spec]} (cc/stackfile name)]
    (go
      (try
        (api/stack-login owner spec)
        (dcli/stack-deploy name (:compose spec))
        (channel/broadcast-info owner (str "Stack " name " redeploy finished."))
        (catch Exception ex
          (channel/broadcast-error owner (str "Stack " name " redeploy failed.") ex))))))

(defn rollback-stack
  "Rollback stack asynchronously and notify owner abount progress"
  [owner name]
  (let [{:keys [name spec previousSpec] :as stackfile-origin} (cc/stackfile name)]
    (go
      (try
        (api/stack-login owner previousSpec)
        (dcli/stack-deploy name (:compose previousSpec))
        (cc/update-stackfile stackfile-origin {:spec         previousSpec
                                               :previousSpec spec})
        (channel/broadcast-info owner (str "Stack " name " rollback finished."))
        (catch Exception ex
          (channel/broadcast-error owner (str "Stack " name " rollback failed.") ex))))))