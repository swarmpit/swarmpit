(ns swarmpit.routes
  (:require [reitit.core :as r]
            [reitit.coercion.spec :as rss]
            #?(:cljs [reitit.frontend :as rf])
            #?(:clj [swarmpit.handler :as handler])
            #?(:clj [swarmpit.event.handler :as event-handler])))

(def backend
  [["/"
    {:name :index
     :get  (array-map
             #?@(:clj [:handler handler/index]))}]
   ["/events"
    {:name :events
     :get  (array-map
             :parameters {:query {:slt          string?
                                  :subscription string?}}
             #?@(:clj [:handler event-handler/events]))
     :post (array-map
             :parameters {:body any?}
             #?@(:clj [:handler event-handler/event-push]))}]
   ["/version"
    {:name :version
     :get  (array-map
             #?@(:clj [:handler handler/version]))}]
   ["/login"
    {:name :login
     :post (array-map
             #?@(:clj [:handler handler/login]))}]
   ["/slt"
    {:name :slt
     :get  (array-map
             #?@(:clj [:handler handler/slt]))}]
   ["/password"
    {:name :password
     :post (array-map
             :parameters {:body any?}
             #?@(:clj [:handler handler/password]))}]
   ["/api-token"
    {:name   :api-token
     :post   (array-map
               :parameters {:body any?}
               #?@(:clj [:handler handler/api-token-generate]))
     :delete (array-map
               #?@(:clj [:handler handler/api-token-remove]))}]
   ["/initialize"
    {:name :initialize
     :post (array-map
             :parameters {:body any?}
             #?@(:clj [:handler handler/initialize]))}]
   ["/api"
    ["/me"
     {:name :me
      :get  (array-map
              #?@(:clj [:handler handler/me]))}]
    ["/stats"
     {:name :stats
      :get  (array-map
              #?@(:clj [:handler handler/stats]))}]
    ;; Task
    ["/tasks"
     {:name :tasks
      :get  (array-map
              #?@(:clj [:handler handler/tasks]))}]
    ["/tasks/:id"
     {:name :task
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/task]))}]
    ["/tasks/:name/ts"
     {:name :task-ts
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/task-ts]))}]
    ;; Stack
    ["/stacks"
     {:name :stacks
      :get  (array-map
              #?@(:clj [:handler handler/stacks]))
      :post (array-map
              :parameters {:body any?}
              #?@(:clj [:handler handler/stack-create]))}]
    ["/stacks/:name"
     {:name   :stack
      :post   (array-map
                :parameters {:body any?
                             :path {:name string?}}
                #?@(:clj [:handler handler/stack-update]))
      :delete (array-map
                :parameters {:path {:name string?}}
                #?@(:clj [:handler handler/stack-delete]))}]
    ["/stacks/:name/file"
     {:name :stack-file
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-file]))}]
    ["/stacks/:name/compose"
     {:name :stack-compose
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-compose]))}]
    ["/stacks/:name/services"
     {:name :stack-services
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-services]))}]
    ["/stacks/:name/networks"
     {:name :stack-networks
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-networks]))}]
    ["/stacks/:name/volumes"
     {:name :stack-volumes
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-volumes]))}]
    ["/stacks/:name/configs"
     {:name :stack-configs
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-configs]))}]
    ["/stacks/:name/secrets"
     {:name :stack-secrets
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-secrets]))}]
    ["/stacks/:name/redeploy"
     {:name :stack-redeploy
      :post (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-redeploy]))}]
    ["/stacks/:name/rollback"
     {:name :stack-rollback
      :post (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/stack-rollback]))}]
    ;; Admin
    ["/admin"
     ["/users"
      {:name :users
       :get  (array-map
               #?@(:clj [:handler handler/users]))
       :post (array-map
               :parameters {:body any?}
               #?@(:clj [:handler handler/user-create]))}]
     ["/users/:id"
      {:name   :user
       :get    (array-map
                 :parameters {:path {:id string?}}
                 #?@(:clj [:handler handler/user]))
       :delete (array-map
                 :parameters {:path {:id string?}}
                 #?@(:clj [:handler handler/user-delete]))
       :post   (array-map
                 :parameters {:body any?
                              :path {:id string?}}
                 #?@(:clj [:handler handler/user-update]))}]]
    ;; Secret
    ["/secrets"
     {:name :secrets
      :get  (array-map
              #?@(:clj [:handler handler/secrets]))
      :post (array-map
              :parameters {:body any?}
              #?@(:clj [:handler handler/secret-create]))}]
    ["/secrets/:id"
     {:name   :secret
      :get    (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/secret]))
      :post   (array-map
                :parameters {:body any?
                             :path {:id string?}}
                #?@(:clj [:handler handler/secret-update]))
      :delete (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/secret-delete]))}]
    ["/secrets/:id/services"
     {:name :secret-services
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/secret-services]))}]
    ;; Config
    ["/configs"
     {:name :configs
      :get  (array-map
              #?@(:clj [:handler handler/configs]))
      :post (array-map
              :parameters {:body any?}
              #?@(:clj [:handler handler/config-create]))}]
    ["/configs/:id"
     {:name   :config
      :get    (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/config]))
      :delete (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/config-delete]))}]
    ["/configs/:id/services"
     {:name :config-services
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/config-services]))}]
    ;; Public
    ["/public/repositories"
     {:name :public-repositories
      :get  (array-map
              :parameters {:query {:query string?
                                   :page  int?}}
              #?@(:clj [:handler handler/public-repositories]))}]
    ;; Registry
    ["/registry/:registryType"
     {:name :registries
      :get  (array-map
              :parameters {:path {:registryType string?}}
              #?@(:clj [:handler handler/registries]))
      :post (array-map
              :parameters {:body any?
                           :path {:registryType string?}}
              #?@(:clj [:handler handler/registry-create]))}]
    ["/registry/:registryType/:id"
     {:name   :registry
      :get    (array-map
                :parameters {:path {:registryType string?
                                    :id           string?}}
                #?@(:clj [:handler handler/registry]))
      :delete (array-map
                :parameters {:path {:registryType string?
                                    :id           string?}}
                #?@(:clj [:handler handler/registry-delete]))
      :post   (array-map
                :parameters {:body any?
                             :path {:registryType string?
                                    :id           string?}}
                #?@(:clj [:handler handler/registry-update]))}]
    ["/registry/:registryType/:id/repositories"
     {:name :registry-repositories
      :get  (array-map
              :parameters {:path {:registryType string?
                                  :id           string?}}
              #?@(:clj [:handler handler/registry-repositories]))}]

    ;; Repository
    ["/repository"
     ["/tags"
      {:name :repository-tags
       :get  (array-map
               :parameters {:query {:repository string?}}
               #?@(:clj [:handler handler/repository-tags]))}]
     ["/ports"
      {:name :repository-ports
       :get  (array-map
               :parameters {:query {:repository    string?
                                    :repositoryTag string?}}
               #?@(:clj [:handler handler/repository-ports]))}]]
    ;; Network
    ["/networks"
     {:name :networks
      :get  (array-map
              #?@(:clj [:handler handler/networks]))
      :post (array-map
              :parameters {:body any?}
              #?@(:clj [:handler handler/network-create]))}]
    ["/networks/:id"
     {:name   :network
      :get    (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/network]))
      :delete (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/network-delete]))}]
    ["/networks/:id/services"
     {:name :network-services
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/network-services]))}]
    ;; Nodes
    ["/nodes"
     {:name :nodes
      :get  (array-map
              #?@(:clj [:handler handler/nodes]))}]
    ["/nodes/ts"
     {:name :nodes-ts
      :get  (array-map
              #?@(:clj [:handler handler/nodes-ts]))}]
    ["/nodes/:id"
     {:name :node
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/node]))
      :post (array-map
              :parameters {:body any?
                           :path {:id string?}}
              #?@(:clj [:handler handler/node-update]))}]
    ["/nodes/:id/tasks"
     {:name :node-tasks
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/node-tasks]))}]
    ;; Service
    ["/services"
     {:name :services
      :get  (array-map
              #?@(:clj [:handler handler/services]))
      :post (array-map
              :parameters {:body any?}
              #?@(:clj [:handler handler/service-create]))}]
    ["/services/:id"
     {:name   :service
      :get    (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/service]))
      :post   (array-map
                :parameters {:body any?
                             :path {:id string?}}
                #?@(:clj [:handler handler/service-update]))
      :delete (array-map
                :parameters {:path {:id string?}}
                #?@(:clj [:handler handler/service-delete]))}]
    ["/services/:id/logs"
     {:name :service-logs
      :get  (array-map
              :parameters {:path  {:id string?}
                           :query {:since string?}}
              #?@(:clj [:handler handler/service-logs]))}]
    ["/services/:id/networks"
     {:name :service-networks
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/service-networks]))}]
    ["/services/:id/tasks"
     {:name :service-tasks
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/service-tasks]))}]
    ["/services/:id/compose"
     {:name :service-compose
      :get  (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/service-compose]))}]
    ["/services/:id/redeploy"
     {:name :service-redeploy
      :post (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/service-redeploy]))}]
    ["/services/:id/rollback"
     {:name :service-rollback
      :post (array-map
              :parameters {:path {:id string?}}
              #?@(:clj [:handler handler/service-rollback]))}]
    ;; Volume
    ["/volumes"
     {:name :volumes
      :get  (array-map
              #?@(:clj [:handler handler/volumes]))
      :post (array-map
              :parameters {:body any?}
              #?@(:clj [:handler handler/volume-create]))}]
    ["/volumes/:name"
     {:name   :volume
      :get    (array-map
                :parameters {:path {:name string?}}
                #?@(:clj [:handler handler/volume]))
      :delete (array-map
                :parameters {:path {:name string?}}
                #?@(:clj [:handler handler/volume-delete]))}]
    ["/volumes/:name/services"
     {:name :volume-services
      :get  (array-map
              :parameters {:path {:name string?}}
              #?@(:clj [:handler handler/volume-services]))}]
    ;; Placement
    ["/placement"
     {:name :placement
      :get  (array-map
              #?@(:clj [:handler handler/placement]))}]
    ;; Labels
    ["/labels"
     ["/service"
      {:name :labels-service
       :get  (array-map
               #?@(:clj [:handler handler/labels-service]))}]]
    ;; Plugin
    ["/plugin"
     ["/network"
      {:name :plugin-network
       :get  (array-map
               #?@(:clj [:handler handler/plugin-network]))}]
     ["/log"
      {:name :plugin-log
       :get  (array-map
               #?@(:clj [:handler handler/plugin-log]))}]
     ["/volume"
      {:name :plugin-volume
       :get  (array-map
               #?@(:clj [:handler handler/plugin-volume]))}]]]])

(def backend-router (r/router backend {:data      {:coercion rss/coercion}
                                       :conflicts nil}))

(def frontend
  [["/" :index]
   ["/login" :login]
   ["/error" :error]
   ["/unauthorized" :unauthorized]
   ["/account-settings" :account-settings]
   ;; Service
   ["/services" :service-list]
   ["/services/create/wizard"
    ["/image" :service-create-image]
    ["/config" :service-create-config]]
   ["/services/:id" :service-info]
   ["/services/:id/edit" :service-edit]
   ["/services/:id/log" :service-log]
   ["/services/:id/log/:taskId" :service-task-log]
   ;; Stack
   ["/stacks" :stack-list]
   ["/stacks/create" :stack-create]
   ["/stacks/:name" :stack-info]
   ["/stacks/:name/previous" :stack-previous]
   ["/stacks/:name/last" :stack-last]
   ["/stacks/:name/compose" :stack-compose]
   ;; Network
   ["/networks" :network-list]
   ["/networks/create" :network-create]
   ["/networks/:id" :network-info]
   ;; Volume
   ["/volumes" :volume-list]
   ["/volumes/create" :volume-create]
   ["/volumes/:name" :volume-info]
   ;; Secret
   ["/secrets" :secret-list]
   ["/secrets/create" :secret-create]
   ["/secrets/:id" :secret-info]
   ;; Config
   ["/configs" :config-list]
   ["/configs/create" :config-create]
   ["/configs/:id" :config-info]
   ;; Node
   ["/nodes" :node-list]
   ["/nodes/:id" :node-info]
   ["/nodes/:id/edit" :node-edit]
   ;; Task
   ["/tasks" :task-list]
   ["/tasks/:id" :task-info]
   ;; Registry
   ["/registries" :registry-list]
   ["/registries/create" :registry-create]
   ["/registries/:registryType/:id" :registry-info]
   ["/registries/:registryType/:id/edit" :registry-edit]
   ;; User
   ["/users" :user-list]
   ["/users/create" :user-create]
   ["/users/:id" :user-info]
   ["/users/:id/edit" :user-edit]])

#?(:cljs
   (def frontend-router (rf/router frontend {:data      {:coercion rss/coercion}
                                             :conflicts nil})))

#?(:cljs
   (defn path-for-frontend
     ([handler]
      (path-for-frontend handler nil nil))
     ([handler params]
      (path-for-frontend handler params nil))
     ([handler params query]
      (str "#" (-> frontend-router
                   (rf/match-by-name handler params)
                   (r/match->path query))))))

(defn path-for-backend
  ([handler]
   (path-for-backend handler nil nil))
  ([handler params]
   (path-for-backend handler params nil))
  ([handler params query]
   (-> backend-router
       (r/match-by-name handler params)
       (r/match->path query))))