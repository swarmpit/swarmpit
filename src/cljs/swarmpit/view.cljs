(ns swarmpit.view
  (:require [swarmpit.component.state :as state]
            [swarmpit.event.source :as eventsource]
            [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-403 :as page-403]
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

;; Dispatch to view based on route handler and
;; 1) reset current form data
;; 2) subscribe view
(defmulti dispatch (fn [route]
                     (eventsource/subscribe! route)
                     (state/set-value nil [:form])
                     (:handler route)))

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
  [{:keys [params]}]
  (service-list/form params))

(defmethod dispatch :service-info
  [{:keys [params]}]
  (service-info/form params))

(defmethod dispatch :service-log
  [{:keys [params]}]
  (service-log/form params))

(defmethod dispatch :service-create-image
  [_]
  (service-image/form))

(defmethod dispatch :service-create-config
  [{:keys [params]}]
  (service-config/form params))

(defmethod dispatch :service-edit
  [{:keys [params]}]
  (service-edit/form params))

;;; Network view

(defmethod dispatch :network-list
  [_]
  (network-list/form))

(defmethod dispatch :network-info
  [{:keys [params]}]
  (network-info/form params))

(defmethod dispatch :network-create
  [_]
  (network-create/form))

;;; Node view

(defmethod dispatch :node-list
  [_]
  (node-list/form))

;;; Volume view

(defmethod dispatch :volume-list
  [_]
  (volume-list/form))

(defmethod dispatch :volume-info
  [{:keys [params]}]
  (volume-info/form params))

(defmethod dispatch :volume-create
  [_]
  (volume-create/form))

;;; Secret view

(defmethod dispatch :secret-list
  [_]
  (secret-list/form))

(defmethod dispatch :secret-info
  [{:keys [params]}]
  (secret-info/form params))

(defmethod dispatch :secret-create
  [_]
  (secret-create/form))

;;; Task view

(defmethod dispatch :task-list
  [_]
  (task-list/form))

(defmethod dispatch :task-info
  [{:keys [params]}]
  (task-info/form params))

;;; User view

(defmethod dispatch :user-list
  [_]
  (user-list/form))

(defmethod dispatch :user-info
  [{:keys [params]}]
  (user-info/form params))

(defmethod dispatch :user-create
  [_]
  (user-create/form))

(defmethod dispatch :user-edit
  [{:keys [params]}]
  (user-edit/form params))

;;; Registry view

(defmethod dispatch :registry-list
  [_]
  (registry-list/form))

(defmethod dispatch :registry-info
  [{:keys [params]}]
  (registry-info/form params))

(defmethod dispatch :registry-create
  [_]
  (registry-create/form))

(defmethod dispatch :registry-edit
  [{:keys [params]}]
  (registry-edit/form params))

;;; Dockerhub user view

(defmethod dispatch :dockerhub-user-list
  [_]
  (dockerhub-list/form))

(defmethod dispatch :dockerhub-user-info
  [{:keys [params]}]
  (dockerhub-info/form params))

(defmethod dispatch :dockerhub-user-create
  [_]
  (dockerhub-create/form))

(defmethod dispatch :dockerhub-user-edit
  [{:keys [params]}]
  (dockerhub-edit/form params))