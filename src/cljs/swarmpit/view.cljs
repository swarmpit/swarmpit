(ns swarmpit.view
  (:require [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-401 :as page-401]
            [swarmpit.component.page-404 :as page-404]
            [swarmpit.component.page-error :as page-error]
            [swarmpit.component.password :as password]
            [swarmpit.component.service.create-config :as service-config]
            [swarmpit.component.service.create-image :as service-image]
            [swarmpit.component.service.edit :as service-edit]
            [swarmpit.component.service.info :as service-info]
            [swarmpit.component.service.log :as service-log]
            [swarmpit.component.service.list :as service-list]
            [swarmpit.component.network.create :as network-create]
            [swarmpit.component.network.info :as network-info]
            [swarmpit.component.network.list :as network-list]
            [swarmpit.component.volume.create :as volume-create]
            [swarmpit.component.volume.info :as volume-info]
            [swarmpit.component.volume.list :as volume-list]
            [swarmpit.component.secret.create :as secret-create]
            [swarmpit.component.secret.info :as secret-info]
            [swarmpit.component.secret.list :as secret-list]
            [swarmpit.component.node.list :as node-list]
            [swarmpit.component.task.list :as task-list]
            [swarmpit.component.task.info :as task-info]
            [swarmpit.component.user.list :as user-list]
            [swarmpit.component.user.info :as user-info]
            [swarmpit.component.user.create :as user-create]
            [swarmpit.component.user.edit :as user-edit]
            [swarmpit.component.registry.info :as registry-info]
            [swarmpit.component.registry.list :as registry-list]
            [swarmpit.component.registry.create :as registry-create]
            [swarmpit.component.registry.edit :as registry-edit]
            [swarmpit.component.dockerhub.info :as dockerhub-info]
            [swarmpit.component.dockerhub.list :as dockerhub-list]
            [swarmpit.component.dockerhub.create :as dockerhub-create]
            [swarmpit.component.dockerhub.edit :as dockerhub-edit]))

(defmulti dispatch (fn [route] (:handler route)))

(defmethod dispatch nil
  [_]
  (page-404/form))

(defmethod dispatch :not-found
  [_]
  (page-404/form))

(defmethod dispatch :unauthorized
  [_]
  (page-401/form))

(defmethod dispatch :error
  [_]
  (page-error/form))

(defmethod dispatch :login
  [_]
  (page-login/form))

(defmethod dispatch :password
  [_]
  (password/form))

;;; Service view

(defmethod dispatch :service-list
  [{:keys [data]}]
  (service-list/form data))

(defmethod dispatch :service-info
  [{:keys [data]}]
  (service-info/form data))

(defmethod dispatch :service-log
  [{:keys [data]}]
  (service-log/log-handler data)
  (service-log/form data))

(defmethod dispatch :service-create-image
  [{:keys [data]}]
  (service-image/form data))

(defmethod dispatch :service-create-config
  [{:keys [data]}]
  (service-config/form data))

(defmethod dispatch :service-edit
  [{:keys [data]}]
  (service-edit/form data))

;;; Network view

(defmethod dispatch :network-list
  [{:keys [data]}]
  (network-list/form data))

(defmethod dispatch :network-info
  [{:keys [data]}]
  (network-info/form data))

(defmethod dispatch :network-create
  [_]
  (network-create/network-plugin-handler)
  (network-create/form))

;;; Node view

(defmethod dispatch :node-list
  [{:keys [data]}]
  (node-list/form data))

;;; Volume view

(defmethod dispatch :volume-list
  [{:keys [data]}]
  (volume-list/form data))

(defmethod dispatch :volume-info
  [{:keys [data]}]
  (volume-info/form data))

(defmethod dispatch :volume-create
  [_]
  (volume-create/volume-plugin-handler)
  (volume-create/form))

;;; Secret view

(defmethod dispatch :secret-list
  [{:keys [data]}]
  (secret-list/form data))

(defmethod dispatch :secret-info
  [{:keys [data]}]
  (secret-info/form data))

(defmethod dispatch :secret-create
  [_]
  (secret-create/form))

;;; Task view

(defmethod dispatch :task-list
  [{:keys [data]}]
  (task-list/form data))

(defmethod dispatch :task-info
  [{:keys [data]}]
  (task-info/form data))

;;; User view

(defmethod dispatch :user-list
  [{:keys [data]}]
  (user-list/form data))

(defmethod dispatch :user-info
  [{:keys [data]}]
  (user-info/form data))

(defmethod dispatch :user-create
  [_]
  (user-create/form))

(defmethod dispatch :user-edit
  [{:keys [data]}]
  (user-edit/form data))

;;; Registry view

(defmethod dispatch :registry-list
  [{:keys [data]}]
  (registry-list/form data))

(defmethod dispatch :registry-info
  [{:keys [data]}]
  (registry-info/form data))

(defmethod dispatch :registry-create
  [_]
  (registry-create/form))

(defmethod dispatch :registry-edit
  [{:keys [data]}]
  (registry-edit/form data))

;;; Dockerhub user view

(defmethod dispatch :dockerhub-user-list
  [{:keys [data]}]
  (dockerhub-list/form data))

(defmethod dispatch :dockerhub-user-info
  [{:keys [data]}]
  (dockerhub-info/form data))

(defmethod dispatch :dockerhub-user-create
  [_]
  (dockerhub-create/form))

(defmethod dispatch :dockerhub-user-edit
  [{:keys [data]}]
  (dockerhub-edit/form data))