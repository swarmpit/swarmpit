(ns swarmpit.view-actions
  (:require [swarmpit.component.registry.list :as registry-list]
            [swarmpit.component.stack.list :as stack-list]
            [swarmpit.component.service.log :as service-log]
            [swarmpit.component.service.list :as service-list]
            [swarmpit.component.network.list :as network-list]
            [swarmpit.component.volume.list :as volume-list]
            [swarmpit.component.secret.list :as secret-list]
            [swarmpit.component.config.list :as config-list]
            [swarmpit.component.node.list :as node-list]
            [swarmpit.component.task.list :as task-list]
            [swarmpit.component.user.list :as user-list]))

(defmulti render (fn [route] (:handler route)))

(defmethod render :default
  [_])

(defmethod render :index
  [_]
  {:title "Home"})

(defmethod render :account-settings
  [_]
  {:title "Account Settings"})

;;; Distribution view

(defmethod render :registry-list
  [_]
  {:title     "Registries"
   :search-fn registry-list/form-search-fn})

(defmethod render :registry-create
  [_]
  {:title "Registries"})

;;; Stack view

(defmethod render :stack-list
  [_]
  {:title     "Stacks"
   :search-fn stack-list/form-search-fn})

(defmethod render :stack-info
  [_]
  {:title "Stacks"})

(defmethod render :stack-create
  [_]
  {:title "Stacks"})

(defmethod render :stack-last
  [_]
  {:title "Stacks"})

(defmethod render :stack-previous
  [_]
  {:title "Stacks"})

(defmethod render :stack-compose
  [_]
  {:title "Stacks"})

;;; Service view

(defmethod render :service-list
  [_]
  {:title     "Services"
   :search-fn service-list/form-search-fn})

(defmethod render :service-info
  [_]
  {:title "Services"})

(defmethod render :service-log
  [route]
  {:title     "Logs"
   :subtitle  (-> route :params :id)
   :search-fn service-log/form-search-fn})

(defmethod render :service-create-image
  [_]
  {:title "Services"})

(defmethod render :service-create-config
  [_]
  {:title "Services"})

(defmethod render :service-edit
  [_]
  {:title "Services"})

;;; Network view

(defmethod render :network-list
  [_]
  {:title     "Networks"
   :search-fn network-list/form-search-fn})

(defmethod render :network-info
  [_]
  {:title "Networks"})

(defmethod render :network-create
  [_]
  {:title "Networks"})

;;; Node view

(defmethod render :node-list
  [_]
  {:title     "Nodes"
   :search-fn node-list/form-search-fn})

(defmethod render :node-info
  [_]
  {:title "Nodes"})

(defmethod render :node-edit
  [_]
  {:title "Nodes"})

;;; Volume view

(defmethod render :volume-list
  [_]
  {:title     "Volumes"
   :search-fn volume-list/form-search-fn})

(defmethod render :volume-info
  [_]
  {:title "Volumes"})

(defmethod render :volume-create
  [_]
  {:title "Volumes"})

;;; Secret view

(defmethod render :secret-list
  [_]
  {:title     "Secrets"
   :search-fn secret-list/form-search-fn})

(defmethod render :secret-info
  [_]
  {:title "Secrets"})

(defmethod render :secret-create
  [_]
  {:title "Secrets"})

;;; Config view

(defmethod render :config-list
  [_]
  {:title     "Configs"
   :search-fn config-list/form-search-fn})

(defmethod render :config-info
  [_]
  {:title "Configs"})

(defmethod render :config-create
  [_]
  {:title "Configs"})

;;; Task view

(defmethod render :task-list
  [_]
  {:title     "Tasks"
   :search-fn task-list/form-search-fn})

(defmethod render :task-info
  [_]
  {:title "Tasks"})

;;; User view

(defmethod render :user-list
  [_]
  {:title     "Users"
   :search-fn user-list/form-search-fn})

(defmethod render :user-info
  [_]
  {:title "Users"})

(defmethod render :user-create
  [_]
  {:title "Users"})

(defmethod render :user-edit
  [_]
  {:title "Users"})

;;; Registry v2 view

(defmethod render :reg-v2-info
  [_]
  {:title "Registries"})

(defmethod render :reg-v2-edit
  [_]
  {:title "Registries"})

;;; Dockerhub view

(defmethod render :reg-dockerhub-info
  [_]
  {:title "Registries"})

(defmethod render :reg-dockerhub-edit
  [_]
  {:title "Registries"})
