(ns swarmpit.routes
  (:require [reitit.core :as r]
            [reitit.coercion.spec :as rss]
            [swarmpit.routes-spec :as spec]
            [spec-tools.data-spec :as ds]
            [clojure.string :as str]
            #?(:cljs [reitit.frontend :as rf])
            #?(:clj [reitit.swagger :as swagger])
            #?(:clj [swarmpit.version :as version])
            #?(:clj [swarmpit.handler :as handler])
            #?(:clj [swarmpit.event.handler :as event-handler])))

#?(:clj
   (defn api-version []
     (let [version (version/short-info)]
       (clojure.string/replace
         (:version version)
         #"SNAPSHOT"
         (->> (:revision version)
              (take 7)
              (apply str))))))

(def backend
  [["/"
    {:name :index
     :get  (array-map
             :no-doc true
             #?@(:clj [:handler handler/index]))}]
   ["/events"
    {:name :events
     :get  (array-map
             :no-doc true
             :parameters {:query {:slt          string?
                                  :subscription string?}}
             #?@(:clj [:handler event-handler/events]))
     :post (array-map
             :no-doc true
             :parameters {:body any?}
             #?@(:clj [:handler event-handler/event-push]))}]
   ["/version"
    {:name :version
     :get  (array-map
             :no-doc true
             #?@(:clj [:handler handler/version]))}]
   ["/login"
    {:name :login
     :post (array-map
             :no-doc true
             #?@(:clj [:handler handler/login]))}]
   ["/slt"
    {:name :slt
     :get  (array-map
             :no-doc true
             #?@(:clj [:handler handler/slt]))}]
   ["/password"
    {:name :password
     :post (array-map
             :no-doc true
             :parameters {:body any?}
             #?@(:clj [:handler handler/password]))}]
   ["/api-token"
    {:name   :api-token
     :post   (array-map
               :no-doc true
               #?@(:clj [:handler handler/api-token-generate]))
     :delete (array-map
               :no-doc true
               #?@(:clj [:handler handler/api-token-remove]))}]
   ["/initialize"
    {:name :initialize
     :post (array-map
             :no-doc true
             :parameters {:body any?}
             #?@(:clj [:handler handler/initialize]))}]
   ["/api"
    ["/swagger.json"
     {:get (array-map
             :no-doc true
             :swagger {:info
                       (array-map
                         :title "Swarmpit API"
                         :description "Swarmpit backend API description"
                         :contact {:name  "Swarmpit Team"
                                   :email "team@swarmpit.io"
                                   :url   "https://swarmpit.io"}
                         :license {:name "Eclipse Public License"
                                   :url  "http://www.eclipse.org/legal/epl-v10.html"}
                         #?@(:clj [:version (api-version)]))}
             #?@(:clj [:handler (swagger/create-swagger-handler)]))}]
    ["/me"
     {:name    :me
      :swagger {:tags ["me"]}
      :get     (array-map
                 :summary "Logged user info"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        spec/me
                                  :description "Success"}}
                 #?@(:clj [:handler handler/me]))}]
    ["/stats"
     {:name    :stats
      :swagger {:tags ["statistics"]}
      :get     (array-map
                 :summary "Cluster statistics"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        spec/stats
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stats]))}]
    ;; Task
    ["/tasks"
     {:name    :tasks
      :swagger {:tags ["task"]}
      :get     (array-map
                 :summary "Task list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/task]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/tasks]))}]
    ["/tasks/:id"
     {:name    :task
      :swagger {:tags ["task"]}
      :get     (array-map
                 :summary "Task info"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/task
                                  :description "Success"}}
                 #?@(:clj [:handler handler/task]))}]
    ["/tasks/:name/ts"
     {:name    :task-ts
      :swagger {:tags ["task"]}
      :get     (array-map
                 :summary "Task timeseries"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        spec/task-stats
                                  :description "Success"}}
                 #?@(:clj [:handler handler/task-ts]))}]
    ;; Stack
    ["/stacks"
     {:name    :stacks
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        spec/stack
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stacks]))
      :post    (array-map
                 :summary "Create stack"
                 :parameters {:header {:authorization string?}
                              :body   spec/stack-compose}
                 :responses {201 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-create]))}]
    ["/stacks/:name"
     {:name    :stack
      :swagger {:tags ["stack"]}
      :post    (array-map
                 :summary "Edit stack"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}
                              :body   spec/stack-compose}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-update]))
      :delete  (array-map
                 :summary "Delete stack"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-delete]))}]
    ["/stacks/:name/file"
     {:name    :stack-file
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack file"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        spec/stack-file
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-file]))
      :post    (array-map
                 :summary "Create stack file"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}
                              :body   spec/stack-compose}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-file-create]))
      :delete  (array-map
                 :summary "Delete stack file"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-file-delete]))}]
    ["/stacks/:name/compose"
     {:name    :stack-compose
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack compose"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        spec/stack-compose
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-compose]))}]
    ["/stacks/:name/services"
     {:name    :stack-services
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack services"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [spec/service]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-services]))}]
    ["/stacks/:name/tasks"
     {:name    :stack-tasks
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack tasks"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [spec/task]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-tasks]))}]
    ["/stacks/:name/networks"
     {:name    :stack-networks
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack networks"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [spec/network]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-networks]))}]
    ["/stacks/:name/volumes"
     {:name    :stack-volumes
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack volumes"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [(merge spec/volume spec/mount)]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-volumes]))}]
    ["/stacks/:name/configs"
     {:name    :stack-configs
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack configs"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [(dissoc spec/config :data)]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-configs]))}]
    ["/stacks/:name/secrets"
     {:name    :stack-secrets
      :swagger {:tags ["stack"]}
      :get     (array-map
                 :summary "Stack secrets"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [spec/secret]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-secrets]))}]
    ["/stacks/:name/redeploy"
     {:name    :stack-redeploy
      :swagger {:tags ["stack"]}
      :post    (array-map
                 :summary "Redeploy stack"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-redeploy]))}]
    ["/stacks/:name/rollback"
     {:name    :stack-rollback
      :swagger {:tags ["stack"]}
      :post    (array-map
                 :summary "Rollback stack"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-rollback]))}]
    ["/stacks/:name/deactivate"
     {:name    :stack-deactivate
      :swagger {:tags ["stack"]}
      :post    (array-map
                 :summary "Deactivate stack"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/stack-deactivate]))}]
    ;; Admin
    ["/admin"
     {:swagger {:tags ["admin"]}}
     ["/users"
      {:name    :users
       :swagger {:tags ["user"]}
       :get     (array-map
                  :summary "User list"
                  :parameters {:header {:authorization string?}}
                  :responses {200 {:body        [spec/user]
                                   :description "Success"}}
                  #?@(:clj [:handler handler/users]))
       :post    (array-map
                  :summary "Create user"
                  :parameters {:header {:authorization string?}
                               :body   spec/user-create}
                  :responses {201 {:body        {:id string?}
                                   :description "Success"}}
                  #?@(:clj [:handler handler/user-create]))}]
     ["/users/:id"
      {:name    :user
       :swagger {:tags ["user"]}
       :get     (array-map
                  :summary "User info"
                  :parameters {:header {:authorization string?}
                               :path   {:id string?}}
                  :responses {200 {:body        spec/user
                                   :description "Success"}}
                  #?@(:clj [:handler handler/user]))
       :delete  (array-map
                  :summary "Delete user"
                  :parameters {:header {:authorization string?}
                               :path   {:id string?}}
                  :responses {200 {:body        nil
                                   :description "Success"}}
                  #?@(:clj [:handler handler/user-delete]))
       :post    (array-map
                  :summary "Edit user"
                  :parameters {:header {:authorization string?}
                               :path   {:id string?}
                               :body   spec/user-update}
                  :responses {200 {:body        nil
                                   :description "Success"}}
                  #?@(:clj [:handler handler/user-update]))}]]
    ;; Secret
    ["/secrets"
     {:name    :secrets
      :swagger {:tags ["secret"]}
      :get     (array-map
                 :summary "Secret list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/secret]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/secrets]))
      :post    (array-map
                 :summary "Create secret"
                 :parameters {:header {:authorization string?}
                              :body   spec/secret-create}
                 :responses {201 {:body        {:id string?}
                                  :description "Success"}}
                 #?@(:clj [:handler handler/secret-create]))}]
    ["/secrets/:id"
     {:name    :secret
      :swagger {:tags ["secret"]}
      :get     (array-map
                 :summary "Secret info"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/secret
                                  :description "Success"}}
                 #?@(:clj [:handler handler/secret]))
      :post    (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :body   any?
                              :path   {:id string?}}
                 #?@(:clj [:handler handler/secret-update]))
      :delete  (array-map
                 :summary "Delete secret"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/secret-delete]))}]
    ["/secrets/:id/services"
     {:name    :secret-services
      :swagger {:tags ["secret"]}
      :get     (array-map
                 :summary "Secret services"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        [spec/service]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/secret-services]))}]
    ;; Config
    ["/configs"
     {:name    :configs
      :swagger {:tags ["config"]}
      :get     (array-map
                 :summary "Config list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/config]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/configs]))
      :post    (array-map
                 :summary "Create config"
                 :parameters {:header {:authorization string?}
                              :body   spec/config-create}
                 :responses {201 {:body        {:id string?}
                                  :description "Success"}}
                 #?@(:clj [:handler handler/config-create]))}]
    ["/configs/:id"
     {:name    :config
      :swagger {:tags ["config"]}
      :get     (array-map
                 :summary "Config info"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/config
                                  :description "Success"}}
                 #?@(:clj [:handler handler/config]))
      :delete  (array-map
                 :summary "Delete config"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/config-delete]))}]
    ["/configs/:id/services"
     {:name    :config-services
      :swagger {:tags ["config"]}
      :get     (array-map
                 :summary "Config services"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        [spec/service]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/config-services]))}]
    ;; Public
    ["/public/repositories"
     {:name    :public-repositories
      :swagger {:tags ["repository"]}
      :get     (array-map
                 :summary "Public repositories"
                 :parameters {:header {:authorization string?}
                              :query  {:query string?
                                       :page  int?}}
                 :responses {200 {:body        spec/repositories
                                  :description "Success"}}
                 #?@(:clj [:handler handler/public-repositories]))}]
    ;; Registry
    ["/registry/:registryType"
     {:name    :registries
      :swagger {:tags ["registry"]}
      :get     (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :path   {:registryType string?}}
                 #?@(:clj [:handler handler/registries]))
      :post    (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :path   {:registryType string?}
                              :body   any?}
                 #?@(:clj [:handler handler/registry-create]))}]
    ["/registry/:registryType/:id"
     {:name    :registry
      :swagger {:tags ["registry"]}
      :get     (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :path   {:registryType string?
                                       :id           string?}}
                 #?@(:clj [:handler handler/registry]))
      :delete  (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :path   {:registryType string?
                                       :id           string?}}
                 #?@(:clj [:handler handler/registry-delete]))
      :post    (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :path   {:registryType string?
                                       :id           string?}
                              :body   any?}
                 #?@(:clj [:handler handler/registry-update]))}]
    ["/registry/:registryType/:id/repositories"
     {:name    :registry-repositories
      :swagger {:tags ["registry"]}
      :get     (array-map
                 :no-doc true
                 :parameters {:header {:authorization string?}
                              :path   {:registryType string?
                                       :id           string?}}
                 #?@(:clj [:handler handler/registry-repositories]))}]

    ;; Repository
    ["/repository"
     {:swagger {:tags ["repository"]}}
     ["/tags"
      {:name :repository-tags
       :get  (array-map
               :summary "Repository tags"
               :parameters {:header {:authorization string?}
                            :query  {:repository string?}}
               :responses {200 {:body        [string?]
                                :description "Success"}}
               #?@(:clj [:handler handler/repository-tags]))}]
     ["/ports"
      {:name :repository-ports
       :get  (array-map
               :summary "Repository ports"
               :parameters {:header {:authorization string?}
                            :query  {:repository    string?
                                     :repositoryTag string?}}
               :responses {200 {:body        [{:containerPort number?
                                               :protocol      string?
                                               :hostPort      number?}]
                                :description "Success"}}
               #?@(:clj [:handler handler/repository-ports]))}]]
    ;; Network
    ["/networks"
     {:name    :networks
      :swagger {:tags ["network"]}
      :get     (array-map
                 :summary "Network list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/network]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/networks]))
      :post    (array-map
                 :summary "Create network"
                 :parameters {:header {:authorization string?}
                              :body   spec/network-create}
                 :responses {201 {:body        {:id string?}
                                  :description "Success"}}
                 #?@(:clj [:handler handler/network-create]))}]
    ["/networks/:id"
     {:name    :network
      :swagger {:tags ["network"]}
      :get     (array-map
                 :summary "Network info"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/network
                                  :description "Success"}}
                 #?@(:clj [:handler handler/network]))
      :delete  (array-map
                 :summary "Delete network"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/network-delete]))}]
    ["/networks/:id/services"
     {:name    :network-services
      :swagger {:tags ["network"]}
      :get     (array-map
                 :summary "Network services"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        [spec/service]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/network-services]))}]
    ;; Nodes
    ["/nodes"
     {:name    :nodes
      :swagger {:tags ["node"]}
      :get     (array-map
                 :summary "Node list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/node]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/nodes]))}]
    ["/nodes/ts"
     {:name    :nodes-ts
      :swagger {:tags ["node"]}
      :get     (array-map
                 :summary "Nodes timeseries"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/node-stats]
                                  :description "Success"}
                             400 {:body        {:error string?}
                                  :description "Statistics disabled"}}
                 #?@(:clj [:handler handler/nodes-ts]))}]
    ["/nodes/:id"
     {:name    :node
      :swagger {:tags ["node"]}
      :get     (array-map
                 :summary "Node info"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/node
                                  :description "Success"}}
                 #?@(:clj [:handler handler/node]))
      :post    (array-map
                 :summary "Edit node"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}
                              :body   spec/node-update}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/node-update]))
      :delete  (array-map
                 :summary "Delete node"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/node-delete]))}]
    ["/nodes/:id/tasks"
     {:name    :node-tasks
      :swagger {:tags ["node"]}
      :get     (array-map
                 :summary "Node tasks"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        [spec/task]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/node-tasks]))}]
    ["/nodes/:id/dashboard"
     {:name    :node-dashboard
      :swagger {:tags ["node"]}
      :post    (array-map
                 :summary "Pin node to dashboard"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/dashboard-pin]))
      :delete  (array-map
                 :summary "Detache node from dashboard"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/dashboard-detach]))}]
    ;; Service
    ["/services"
     {:name    :services
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Service list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/service]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/services]))
      :post    (array-map
                 :summary "Create service"
                 :parameters {:header {:authorization string?}
                              :body   spec/service-create}
                 :responses {201 {:body        {:id string?}
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-create]))}]
    ["/services/ts/cpu"
     {:name    :services-ts-cpu
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Services cpu timeseries"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/service-stats]
                                  :description "Success"}
                             400 {:body        {:error string?}
                                  :description "Statistics disabled"}}
                 #?@(:clj [:handler handler/services-ts-cpu]))}]
    ["/services/ts/memory"
     {:name    :services-ts-memory
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Services ram timeseries"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/service-stats]
                                  :description "Success"}
                             400 {:body        {:error string?}
                                  :description "Statistics disabled"}}
                 #?@(:clj [:handler handler/services-ts-memory]))}]
    ["/services/:id"
     {:name    :service
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Service info"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/service
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service]))
      :post    (array-map
                 :summary "Edit service"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}
                              :body   spec/service-update}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-update]))
      :delete  (array-map
                 :summary "Delete service"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-delete]))}]
    ["/services/:id/logs"
     {:name    :service-logs
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Service logs"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}
                              :query  {:since string?}}
                 :responses {200 {:body        spec/service-logs
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-logs]))}]
    ["/services/:id/networks"
     {:name    :service-networks
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Service networks"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        [spec/network]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-networks]))}]
    ["/services/:id/tasks"
     {:name    :service-tasks
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Service tasks"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        [spec/task]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-tasks]))}]
    ["/services/:id/compose"
     {:name    :service-compose
      :swagger {:tags ["service"]}
      :get     (array-map
                 :summary "Service compose"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        spec/stack-compose
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-compose]))}]
    ["/services/:id/redeploy"
     {:name    :service-redeploy
      :swagger {:tags ["service"]}
      :post    (array-map
                 :summary "Redeploy service"
                 :description "Redeploy service with newest image version or different tag if specified"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}
                              :query  {(ds/opt :tag) string?}}
                 :responses {202 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-redeploy]))}]
    ["/services/:id/rollback"
     {:name    :service-rollback
      :swagger {:tags ["service"]}
      :post    (array-map
                 :summary "Rollback service"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {202 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-rollback]))}]
    ["/services/:id/stop"
     {:name    :service-stop
      :swagger {:tags ["service"]}
      :post    (array-map
                 :summary "Stop service"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/service-stop]))}]
    ["/services/:id/dashboard"
     {:name    :service-dashboard
      :swagger {:tags ["service"]}
      :post    (array-map
                 :summary "Pin service to dashboard"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/dashboard-pin]))
      :delete  (array-map
                 :summary "Detache service from dashboard"
                 :parameters {:header {:authorization string?}
                              :path   {:id string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/dashboard-detach]))}]
    ;; Volume
    ["/volumes"
     {:name    :volumes
      :swagger {:tags ["volume"]}
      :get     (array-map
                 :summary "Volume list"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [spec/volume]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/volumes]))
      :post    (array-map
                 :summary "Create volume"
                 :parameters {:header {:authorization string?}
                              :body   spec/volume-create}
                 :responses {201 {:body        spec/volume
                                  :description "Success"}}
                 #?@(:clj [:handler handler/volume-create]))}]
    ["/volumes/:name"
     {:name    :volume
      :swagger {:tags ["volume"]}
      :get     (array-map
                 :summary "Volume info"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        spec/volume
                                  :description "Success"}}
                 #?@(:clj [:handler handler/volume]))
      :delete  (array-map
                 :summary "Delete volume"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        nil
                                  :description "Success"}}
                 #?@(:clj [:handler handler/volume-delete]))}]
    ["/volumes/:name/services"
     {:name    :volume-services
      :swagger {:tags ["volume"]}
      :get     (array-map
                 :summary "Volume services"
                 :parameters {:header {:authorization string?}
                              :path   {:name string?}}
                 :responses {200 {:body        [spec/service]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/volume-services]))}]
    ;; Placement
    ["/placement"
     {:name    :placement
      :swagger {:tags ["placement"]}
      :get     (array-map
                 :summary "Placement constraints"
                 :parameters {:header {:authorization string?}}
                 :responses {200 {:body        [string?]
                                  :description "Success"}}
                 #?@(:clj [:handler handler/placement]))}]
    ;; Labels
    ["/labels"
     {:swagger {:tags ["label"]}}
     ["/service"
      {:name :labels-service
       :get  (array-map
               :summary "Service labels"
               :parameters {:header {:authorization string?}}
               :responses {200 {:body        [string?]
                                :description "Success"}}
               #?@(:clj [:handler handler/labels-service]))}]]
    ;; Plugin
    ["/plugin"
     {:swagger {:tags ["plugin"]}}
     ["/network"
      {:name :plugin-network
       :get  (array-map
               :summary "Network plugins"
               :parameters {:header {:authorization string?}}
               :responses {200 {:body        [string?]
                                :description "Success"}}
               #?@(:clj [:handler handler/plugin-network]))}]
     ["/log"
      {:name :plugin-log
       :get  (array-map
               :summary "Logging plugins"
               :parameters {:header {:authorization string?}}
               :responses {200 {:body        [string?]
                                :description "Success"}}
               #?@(:clj [:handler handler/plugin-log]))}]
     ["/volume"
      {:name :plugin-volume
       :get  (array-map
               :summary "Volume plugins"
               :parameters {:header {:authorization string?}}
               :responses {200 {:body        [string?]
                                :description "Success"}}
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
   ;; Stack
   ["/stacks" :stack-list]
   ["/stacks/create" :stack-create]
   ["/stacks/:name" :stack-info]
   ["/stacks/:name/previous" :stack-previous]
   ["/stacks/:name/last" :stack-last]
   ["/stacks/:name/compose" :stack-compose]
   ["/stacks/:name/activate" :stack-activate]
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
   (str/replace
     (-> backend-router
         (r/match-by-name handler params)
         (r/match->path query)) #"^/" "")))
