(ns swarmpit.routes
  (:require [bidi.bidi :as b]
            [cemerick.url :refer [map->query]]
            [clojure.string :as str]
            #?(:clj [swarmpit.handler :as handler])
            #?(:clj [swarmpit.event.handler :as event-handler])
            [swarmpit.handler :as handler]
            [swarmpit.handler :as handler])


  ;(:require [bidi.bidi :as b]
  ;          [cemerick.url :refer [map->query]]
  ;          [clojure.string :as str])
  )

(def table
  [["/" {:get {:name    :index
               :handler handler/index}}]
   ["/events" {:get  {:name    :events
                      :handler event-handler/events}
               :post {:name    :event-push
                      :handler event-handler/event-push}}]
   ["/version" {:get {:name    :version
                      :handler handler/version}}]
   ["/login" {:post {:name    :login
                     :handler handler/login}}]
   ["/slt" {:get {:name    :slt
                  :handler handler/slt}}]
   ["/password" {:post {:name    :password
                        :handler handler/password}}]
   ["/api-token" {:post   {:name    :api-token-generate
                           :handler handler/api-token-generate}
                  :delete {:name    :api-token-remove
                           :handler handler/api-token-remove}}]
   ["/initialize" {:post {:name    :initialize
                          :handler handler/initialize}}]
   ["/api"
    ["/me" {:get {:name    :me
                  :handler handler/me}}]
    ["/stats" {:get {:name    :stats
                     :handler handler/stats}}]
    ;; Task
    ["/tasks" {:get {:name    :tasks
                     :handler handler/tasks}}]
    ["/tasks/:id" {:get {:name    :task
                         :handler handler/task}}]
    ["/tasks/:name/ts" {:get {:name    :task-ts
                              :handler handler/task-ts}}]
    ;; Stack
    ["/stacks" {:get  {:name    :stacks
                       :handler handler/stacks}
                :post {:name    :stack-create
                       :handler handler/stack-create}}]
    ["/stacks/:name" {:post   {:name    :stack-update
                               :handler handler/stack-update}
                      :delete {:name    :stack-delete
                               :handler handler/stack-delete}}
     ["/file" {:get {:name    :stack-file
                     :handler handler/stack-file}}]
     ["/compose" {:get {:name    :stack-compose
                        :handler handler/stack-compose}}]
     ["/services" {:get {:name    :stack-services
                         :handler handler/stack-services}}]
     ["/networks" {:get {:name    :stack-networks
                         :handler handler/stack-networks}}]
     ["/volumes" {:get {:name    :stack-volumes
                        :handler handler/stack-volumes}}]
     ["/configs" {:get {:name    :stack-configs
                        :handler handler/stack-configs}}]
     ["/secrets" {:get {:name    :stack-secrets
                        :handler handler/stack-secrets}}]
     ["/redeploy" {:post {:name    :stack-redeploy
                          :handler handler/stack-redeploy}}]
     ["/rollback" {:post {:name    :stack-rollback
                          :handler handler/stack-rollback}}]]
    ;; Admin
    ["/admin"
     ["/users" {:get  {:name    :users
                       :handler handler/users}
                :post {:name    :user-create
                       :handler handler/user-create}}]
     ["/users/:id" {:get    {:name    :user
                             :handler handler/user}
                    :delete {:name    :user-delete
                             :handler handler/user-delete}
                    :post   {:name    :user-update
                             :handler handler/user-update}}]]
    ;; Secret
    ["/secrets" {:get  {:name    :secrets
                        :handler handler/secrets}
                 :post {:name    :secret-create
                        :handler handler/secret-create}}]
    ["/secrets/:id" {:get    {:name    :secret
                              :handler handler/secret}
                     :post   {:name    :secret-update
                              :handler handler/secret-update}
                     :delete {:name    :secret-delete
                              :handler handler/secret-delete}}
     ["/services" {:get {:name    :secret-services
                         :handler handler/secret-services}}]]
    ;; Config
    ["/configs" {:get  {:name    :configs
                        :handler handler/configs}
                 :post {:name    :config-create
                        :handler handler/config-create}}]
    ["/configs/:id" {:get    {:name    :config
                              :handler handler/config}
                     :delete {:name    :config-delete
                              :handler handler/config-delete}}
     ["/services" {:get {:name    :config-services
                         :handler handler/config-services}}]]
    ;; Registry
    ["/registry"
     ["/public/repositories" {:get {:name    :public-repositories
                                    :handler handler/public-repositories}}]
     ["/:registryType" {:get  {:name    :registries
                               :handler handler/registries}
                        :post {:name    :registry-create
                               :handler handler/registry-create}}
      ["/:id" {:get    {:name    :registry
                        :handler handler/registry}
               :delete {:name    :registry-delete
                        :handler handler/registry-delete}
               :post   {:name    :registry-update
                        :handler handler/registry-update}}
       ["/repositories" {:get {:name    :registry-repositories
                               :handler handler/registry-repositories}}]]]]
    ;; Repository
    ["/repository"
     ["/tags" {:get {:name    :repository-tags
                     :handler handler/repository-tags}}]
     ["/ports" {:get {:name    :repository-ports
                      :handler handler/repository-ports}}]]
    ;; Network
    ["/networks" {:get  {:name    :networks
                         :handler handler/networks}
                  :post {:name    :network-create
                         :handler handler/network-create}}]
    ["/networks/:id" {:get    {:name    :network
                               :handler handler/network}
                      :delete {:name    :network-delete
                               :handler handler/network-delete}}
     ["/services" {:get {:name    :network-services
                         :handler handler/network-services}}]]
    ;; Nodes
    ["/nodes" {:get {:name    :nodes
                     :handler handler/nodes}}]
    ["/nodes/ts" {:get {:name    :nodes-ts
                        :handler handler/nodes-ts}}]
    ["/nodes/:id" {:get  {:name    :node
                          :handler handler/node}
                   :post {:name    :node-update
                          :handler handler/node-update}}
     ["/tasks" {:get {:name    :node-tasks
                      :handler handler/node-tasks}}]]
    ;; Service
    ["/services" {:get  {:name    :services
                         :handler handler/services}
                  :post {:name    :service-create
                         :handler handler/service-create}}]
    ["/services/:id" {:get    {:name    :service
                               :handler handler/service}
                      :post   {:name    :service-update
                               :handler handler/service-update}
                      :delete {:name    :service-delete
                               :handler handler/service-delete}}
     ["/logs" {:get {:name    :service-logs
                     :handler handler/service-logs}}]
     ["/networks" {:get {:name    :service-networks
                         :handler handler/service-networks}}]
     ["/tasks" {:get {:name    :service-tasks
                      :handler handler/service-tasks}}]
     ["/compose" {:get {:name    :service-compose
                        :handler handler/service-compose}}]
     ["/redeploy" {:post {:name    :service-redeploy
                          :handler handler/service-redeploy}}]
     ["/rollback" {:post {:name    :service-rollback
                          :handler handler/service-rollback}}]]
    ;; Volume
    ["/volumes" {:get  {:name    :volumes
                        :handler handler/volumes}
                 :post {:name    :volume-create
                        :handler handler/volume-create}}]
    ["/volumes/:name" {:get    {:name    :volume
                                :handler handler/volume}
                       :delete {:name    :volume-delete
                                :handler handler/volume-delete}}
     ["/services" {:get {:name    :volume-services
                         :handler handler/volume-services}}]]
    ;; Placement
    ["/placement" {:get {:name    :placement
                         :handler handler/placement}}]
    ;; Labels
    ["/labels"
     ["/service" {:get {:name    :labels-service
                        :handler handler/labels-service}}]]
    ;; Plugin
    ["/plugin"
     ["/network" {:get {:name    :plugin-network
                        :handler handler/plugin-network}}]
     ["/log" {:get {:name    :plugin-log
                    :handler handler/plugin-log}}]
     ["/volume" {:get {:name    :plugin-volume
                       :handler handler/plugin-volume}}]]]])

(def backend
  ["" {"/"           {:get :index}
       "/events"     {:get  :events
                      :post :event-push}
       "/version"    {:get :version}
       "/login"      {:post :login
                      :get  :index}
       "/slt"        {:get :slt}
       "/password"   {:post :password}
       "/api-token"  {:get    :index
                      :post   :api-token-generate
                      :delete :api-token-remove}
       "/initialize" {:post :initialize}
       "/api"        {"/me"          {:get :me}
                      "/stats"       {:get :stats}
                      "/tasks"       {:get :tasks}
                      "/tasks/"      {:get {[:id]   :task
                                            [:name] {"/ts" :task-ts}}}
                      "/stacks"      {:get  :stacks
                                      :post :stack-create}
                      "/stacks/"     {:get    {[:name] {"/file"     :stack-file
                                                        "/compose"  :stack-compose
                                                        "/services" :stack-services
                                                        "/networks" :stack-networks
                                                        "/volumes"  :stack-volumes
                                                        "/configs"  :stack-configs
                                                        "/secrets"  :stack-secrets}}
                                      :delete {[:name] :stack-delete}
                                      :post   {[:name] {""          :stack-update
                                                        "/redeploy" :stack-redeploy
                                                        "/rollback" :stack-rollback}}}
                      "/admin/"      {"users"  {:get  :users
                                                :post :user-create}
                                      "users/" {:get    {[:id] :user}
                                                :delete {[:id] :user-delete}
                                                :post   {[:id] :user-update}}}
                      "/secrets"     {:get  :secrets
                                      :post :secret-create}
                      "/secrets/"    {:get    {[:id] {""          :secret
                                                      "/services" :secret-services}}
                                      :delete {[:id] :secret-delete}
                                      :post   {[:id] :secret-update}}
                      "/configs"     {:get  :configs
                                      :post :config-create}
                      "/configs/"    {:get    {[:id] {""          :config
                                                      "/services" :config-services}}
                                      :delete {[:id] :config-delete}}
                      "/registry/"   {"public"        {:get {"/repositories" :public-repositories}}
                                      [:registryType] {""  {:get  :registries
                                                            :post :registry-create}
                                                       "/" {:get    {[:id "/repositories"] :registry-repositories
                                                                     [:id]                 :registry}
                                                            :delete {[:id] :registry-delete}
                                                            :post   {[:id] :registry-update}}}}
                      "/repository/" {:get {"tags"  :repository-tags
                                            "ports" :repository-ports}}
                      "/networks"    {:get  :networks
                                      :post :network-create}
                      "/networks/"   {:get    {[:id] {""          :network
                                                      "/services" :network-services}}
                                      :delete {[:id] :network-delete}}
                      "/nodes"       {:get :nodes}
                      "/nodes/"      {:get  {"ts"  :nodes-ts
                                             [:id] {""       :node
                                                    "/tasks" :node-tasks}}
                                      :post {[:id] :node-update}}
                      "/services"    {:get  :services
                                      :post :service-create}
                      "/services/"   {:get    {[:id] {""          :service
                                                      "/logs"     :service-logs
                                                      "/networks" :service-networks
                                                      "/tasks"    :service-tasks
                                                      "/compose"  :service-compose}}
                                      :delete {[:id] :service-delete}
                                      :post   {[:id] {""          :service-update
                                                      "/redeploy" :service-redeploy
                                                      "/rollback" :service-rollback}}}
                      "/volumes"     {:get  :volumes,
                                      :post :volume-create}
                      "/volumes/"    {:get    {[:name] {""          :volume
                                                        "/services" :volume-services}}
                                      :delete {[:name] :volume-delete}}
                      "/placement"   {:get :placement}
                      "/labels/"     {:get {"service" :labels-service}}
                      "/plugin/"     {:get {"network" :plugin-network
                                            "log"     :plugin-log
                                            "volume"  :plugin-volume}}}}])

(def frontend ["" {"/"                 :index
                   "/login"            :login
                   "/error"            :error
                   "/unauthorized"     :unauthorized
                   "/account-settings" :account-settings
                   "/services"         {""               :service-list
                                        "/create/wizard" {"/image"  :service-create-image
                                                          "/config" :service-create-config}
                                        ["/" :id]        {""      :service-info
                                                          "/edit" :service-edit
                                                          "/log"  {""            :service-log
                                                                   ["/" :taskId] :service-task-log}}}
                   "/stacks"           {""                      :stack-list
                                        "/create"               :stack-create
                                        ["/" :name]             :stack-info
                                        ["/" :name "/previous"] :stack-previous
                                        ["/" :name "/last"]     :stack-last
                                        ["/" :name "/compose"]  :stack-compose}
                   "/networks"         {""        :network-list
                                        "/create" :network-create
                                        ["/" :id] :network-info}
                   "/volumes"          {""          :volume-list
                                        "/create"   :volume-create
                                        ["/" :name] :volume-info}
                   "/secrets"          {""        :secret-list
                                        "/create" :secret-create
                                        ["/" :id] :secret-info}
                   "/configs"          {""        :config-list
                                        "/create" :config-create
                                        ["/" :id] :config-info}
                   "/nodes"            {""                :node-list
                                        ["/" :id]         :node-info
                                        ["/" :id "/edit"] :node-edit}
                   "/tasks"            {""        :task-list
                                        ["/" :id] :task-info}
                   "/registries"       {""        :registry-list
                                        "/create" :registry-create}
                   "/registries/"      {[:registryType] {["/" :id]         :registry-info
                                                         ["/" :id "/edit"] :registry-edit}}
                   "/users"            {""                :user-list
                                        "/create"         :user-create
                                        ["/" :id]         :user-info
                                        ["/" :id "/edit"] :user-edit}}])

(defn- path
  [routes prefix handler params query]
  (let [path (b/unmatch-pair routes {:handler handler
                                     :params  params})]
    (if (some? query)
      (str prefix path "?" (map->query query))
      (str prefix path))))

(defn path-for-frontend
  ([handler] (path-for-frontend handler {} nil))
  ([handler params] (path-for-frontend handler params nil))
  ([handler params query] (path frontend "#" handler params query)))

(defn path-for-backend
  ([handler] (path-for-backend handler {} nil))
  ([handler params] (path-for-backend handler params nil))
  ([handler params query] (str/replace (path backend "" handler params query) #"^/" "")))
