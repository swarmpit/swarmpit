(ns swarmpit.view
  (:require [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-403 :as page-403]
            [swarmpit.component.page-404 :as page-404]
            [swarmpit.component.page-error :as page-error]
            [swarmpit.component.dashboard :as dashboard]
            [swarmpit.component.account-settings :as account-settings]
            [swarmpit.component.registry.list :as registry-list]
            [swarmpit.component.registry.create :as registry-create]
            [swarmpit.component.stack.edit :as stack-edit]
            [swarmpit.component.stack.compose :as stack-compose]
            [swarmpit.component.stack.activate :as stack-activate]
            [swarmpit.component.stack.create :as stack-create]
            [swarmpit.component.stack.info :as stack-info]
            [swarmpit.component.stack.list :as stack-list]
            [swarmpit.component.service.create-config :as service-config]
            [swarmpit.component.service.create-image :as service-image]
            [swarmpit.component.service.edit :as service-edit]
            [swarmpit.component.service.info :as service-info]
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
            [swarmpit.component.node.edit :as node-edit]
            [swarmpit.component.task.list :as task-list]
            [swarmpit.component.task.info :as task-info]
            [swarmpit.component.user.list :as user-list]
            [swarmpit.component.user.info :as user-info]
            [swarmpit.component.user.create :as user-create]
            [swarmpit.component.user.edit :as user-edit]
            [swarmpit.component.registry-v2.info :as reg-v2-info]
            [swarmpit.component.registry-v2.edit :as reg-v2-edit]
            [swarmpit.component.registry-gitlab.info :as reg-gitlab-info]
            [swarmpit.component.registry-gitlab.edit :as reg-gitlab-edit]
            [swarmpit.component.registry-ecr.info :as reg-ecr-info]
            [swarmpit.component.registry-ecr.edit :as reg-ecr-edit]
            [swarmpit.component.registry-acr.info :as reg-acr-info]
            [swarmpit.component.registry-acr.edit :as reg-acr-edit]
            [swarmpit.component.registry-dockerhub.info :as reg-dockerhub-info]
            [swarmpit.component.registry-dockerhub.edit :as reg-dockerhub-edit]))

(defmulti dispatch (fn [route] (:handler route)))

(defmethod dispatch :index
  [route]
  (dashboard/form route))

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

(defmethod dispatch :account-settings
  [_]
  (account-settings/form))

;;; Registry view

(defmethod dispatch :registry-list
  [route]
  (registry-list/form route))

(defmethod dispatch :registry-create
  [route]
  (registry-create/form route))

(defmethod dispatch :registry-info
  [{:keys [params] :as route}]
  (case (:registryType params)
    "v2" (reg-v2-info/form route)
    "dockerhub" (reg-dockerhub-info/form route)
    "ecr" (reg-ecr-info/form route)
    "acr" (reg-acr-info/form route)
    "gitlab" (reg-gitlab-info/form route)))

(defmethod dispatch :registry-edit
  [{:keys [params] :as route}]
  (case (:registryType params)
    "v2" (reg-v2-edit/form route)
    "dockerhub" (reg-dockerhub-edit/form route)
    "ecr" (reg-ecr-edit/form route)
    "acr" (reg-acr-edit/form route)
    "gitlab" (reg-gitlab-edit/form route)))

;;; Stack view

(defmethod dispatch :stack-list
  [route]
  (stack-list/form route))

(defmethod dispatch :stack-info
  [route]
  (stack-info/form route))

(defmethod dispatch :stack-create
  [route]
  (stack-create/form route))

(defmethod dispatch :stack-last
  [route]
  (stack-edit/form-last route))

(defmethod dispatch :stack-previous
  [route]
  (stack-edit/form-previous route))

(defmethod dispatch :stack-compose
  [route]
  (stack-compose/form route))

(defmethod dispatch :stack-activate
  [route]
  (stack-activate/form route))

;;; Service view

(defmethod dispatch :service-list
  [route]
  (service-list/form route))

(defmethod dispatch :service-info
  [route]
  (service-info/form route))

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

(defmethod dispatch :node-edit
  [route]
  (node-edit/form route))

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
