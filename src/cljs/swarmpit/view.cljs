(ns swarmpit.view
  (:require [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-403 :as page-403]
            [swarmpit.component.page-404 :as page-404]
            [swarmpit.component.page-error :as page-error]
            [swarmpit.component.password :as password]
            [swarmpit.component.service.create-config :as service-config]
            [swarmpit.component.service.create-image :as service-image]

            [swarmpit.component.stack.editor :as stack-editor]

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
            [swarmpit.component.config.create :as config-create]
            [swarmpit.component.config.info :as config-info]
            [swarmpit.component.config.list :as config-list]
            [swarmpit.component.node.list :as node-list]
            [swarmpit.component.node.info :as node-info]
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
  (page-403/form))

(defmethod dispatch :error
  [route]
  (page-error/form route))

(defmethod dispatch :login
  [_]
  (page-login/form))

(defmethod dispatch :password
  [_]
  (password/form))

;;; Stack view

(defmethod dispatch :stack-create
  [{:keys [data]}]
  (stack-editor/form data))

;;; Service view

(defmethod dispatch :service-list
  [route]
  (service-list/form route))

(defmethod dispatch :service-info
  [route]
  (service-info/form route))

(defmethod dispatch :service-log
  [route]
  (service-log/form route))

(defmethod dispatch :service-create-image
  [route]
  (service-image/form route))

(defmethod dispatch :service-create-config
  [route]
  (service-config/form route))

(defmethod dispatch :service-edit
  [route]
  (service-edit/form route))

;;; Network view

(defmethod dispatch :network-list
  [route]
  (network-list/form route))

(defmethod dispatch :network-info
  [route]
  (network-info/form route))

(defmethod dispatch :network-create
  [route]
  (network-create/form route))

;;; Node view

(defmethod dispatch :node-list
  [route]
  (node-list/form route))

(defmethod dispatch :node-info
  [route]
  (node-info/form route))

;;; Volume view

(defmethod dispatch :volume-list
  [route]
  (volume-list/form route))

(defmethod dispatch :volume-info
  [route]
  (volume-info/form route))

(defmethod dispatch :volume-create
  [route]
  (volume-create/form route))

;;; Secret view

(defmethod dispatch :secret-list
  [route]
  (secret-list/form route))

(defmethod dispatch :secret-info
  [route]
  (secret-info/form route))

(defmethod dispatch :secret-create
  [route]
  (secret-create/form route))

;;; Config view

(defmethod dispatch :config-list
  [route]
  (config-list/form route))

(defmethod dispatch :config-info
  [route]
  (config-info/form route))

(defmethod dispatch :config-create
  [route]
  (config-create/form route))

;;; Task view

(defmethod dispatch :task-list
  [route]
  (task-list/form route))

(defmethod dispatch :task-info
  [route]
  (task-info/form route))

;;; User view

(defmethod dispatch :user-list
  [route]
  (user-list/form route))

(defmethod dispatch :user-info
  [route]
  (user-info/form route))

(defmethod dispatch :user-create
  [route]
  (user-create/form route))

(defmethod dispatch :user-edit
  [route]
  (user-edit/form route))

;;; Registry view

(defmethod dispatch :registry-list
  [route]
  (registry-list/form route))

(defmethod dispatch :registry-info
  [route]
  (registry-info/form route))

(defmethod dispatch :registry-create
  [route]
  (registry-create/form route))

(defmethod dispatch :registry-edit
  [route]
  (registry-edit/form route))

;;; Dockerhub user view

(defmethod dispatch :dockerhub-user-list
  [route]
  (dockerhub-list/form route))

(defmethod dispatch :dockerhub-user-info
  [route]
  (dockerhub-info/form route))

(defmethod dispatch :dockerhub-user-create
  [route]
  (dockerhub-create/form route))

(defmethod dispatch :dockerhub-user-edit
  [route]
  (dockerhub-edit/form route))