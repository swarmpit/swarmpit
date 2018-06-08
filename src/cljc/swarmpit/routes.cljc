(ns swarmpit.routes
  (:require [bidi.bidi :as b]
            [cemerick.url :refer [map->query]]
            [clojure.string :as str]))

(def backend
  ["" {"/"              {:get :index}
       "/events"        {:get  :events
                         :post :event-push}
       "/version"       {:get :version}
       "/login"         {:post :login}
       "/slt"           {:get :slt}
       "/password"      {:post :password}
       "/me"            {:get :me}
       "/api-token"     {:post   :api-token-generate
                         :delete :api-token-remove}
       "/repository/"   {:get {"tags"  :repository-tags
                               "ports" :repository-ports}}
       "/distribution/" {"public"     {:get {"/repositories" :public-repositories}}
                         "dockerhub"  {""  {:get  :dockerhub-users
                                            :post :dockerhub-user-create}
                                       "/" {:get    {[:id "/repositories"] :dockerhub-repositories
                                                     [:id]                 :dockerhub-user}
                                            :delete {[:id] :dockerhub-user-delete}
                                            :post   {[:id] :dockerhub-user-update}}}
                         "registries" {""  {:get  :registries
                                            :post :registry-create}
                                       "/" {:get    {[:id "/repositories"] :registry-repositories
                                                     [:id]                 :registry}
                                            :delete {[:id] :registry-delete}
                                            :post   {[:id] :registry-update}}}}
       "/stacks"        {:get  :stacks
                         :post :stack-create}
       "/stacks/"       {:get    {[:name] {"/file"     :stack-file
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
       "/services"      {:get  :services
                         :post :service-create}
       "/services/"     {:get    {[:id] {""          :service
                                         "/logs"     :service-logs
                                         "/networks" :service-networks
                                         "/tasks"    :service-tasks
                                         "/compose"  :service-compose}}
                         :delete {[:id] :service-delete}
                         :post   {[:id] {""          :service-update
                                         "/redeploy" :service-redeploy
                                         "/rollback" :service-rollback}}}
       "/networks"      {:get  :networks
                         :post :network-create}
       "/networks/"     {:get    {[:id] {""          :network
                                         "/services" :network-services}}
                         :delete {[:id] :network-delete}}
       "/volumes"       {:get  :volumes
                         :post :volume-create}
       "/volumes/"      {:get    {[:name] {""          :volume
                                           "/services" :volume-services}}
                         :delete {[:name] :volume-delete}}
       "/secrets"       {:get  :secrets
                         :post :secret-create}
       "/secrets/"      {:get    {[:id] {""          :secret
                                         "/services" :secret-services}}
                         :delete {[:id] :secret-delete}
                         :post   {[:id] :secret-update}}
       "/configs"       {:get  :configs
                         :post :config-create}
       "/configs/"      {:get    {[:id] {""          :config
                                         "/services" :config-services}}
                         :delete {[:id] :config-delete}}
       "/nodes"         {:get :nodes}
       "/placement"     {:get :placement}
       "/nodes/"        {:get  {[:id] {""       :node
                                       "/tasks" :node-tasks}}
                         :post {[:id] :node-update}}
       "/tasks"         {:get :tasks}
       "/tasks/"        {:get {[:id] :task}}
       "/plugin/"       {:get {"network" :plugin-network
                               "log"     :plugin-log
                               "volume"  :plugin-volume}}
       "/labels/"       {:get {"service" :labels-service}}
       "/admin/"        {"users"  {:get  :users
                                   :post :user-create}
                         "users/" {:get    {[:id] :user}
                                   :delete {[:id] :user-delete}
                                   :post   {[:id] :user-update}}}}])

(def frontend ["" {"/"                        :index
                   "/login"                   :login
                   "/api-access"              :api-access
                   "/error"                   :error
                   "/unauthorized"            :unauthorized
                   "/password"                :password
                   "/services"                {""               :service-list
                                               "/create/wizard" {"/image"  :service-create-image
                                                                 "/config" :service-create-config}
                                               ["/" :id]        {""      :service-info
                                                                 "/edit" :service-edit
                                                                 "/log"  :service-log}}
                   "/stacks"                  {""                     :stack-list
                                               "/create"              :stack-create
                                               ["/" :name]            :stack-info
                                               ["/" :name "/edit"]    :stack-edit
                                               ["/" :name "/compose"] :stack-compose}
                   "/networks"                {""        :network-list
                                               "/create" :network-create
                                               ["/" :id] :network-info}
                   "/volumes"                 {""          :volume-list
                                               "/create"   :volume-create
                                               ["/" :name] :volume-info}
                   "/secrets"                 {""        :secret-list
                                               "/create" :secret-create
                                               ["/" :id] :secret-info}
                   "/configs"                 {""        :config-list
                                               "/create" :config-create
                                               ["/" :id] :config-info}
                   "/nodes"                   {""                :node-list
                                               ["/" :id]         :node-info
                                               ["/" :id "/edit"] :node-edit}
                   "/tasks"                   {""        :task-list
                                               ["/" :id] :task-info}
                   "/distribution/registries" {""                :registry-list
                                               "/add"            :registry-create
                                               ["/" :id]         :registry-info
                                               ["/" :id "/edit"] :registry-edit}
                   "/distribution/dockerhub"  {""                :dockerhub-user-list
                                               "/add"            :dockerhub-user-create
                                               ["/" :id]         :dockerhub-user-info
                                               ["/" :id "/edit"] :dockerhub-user-edit}
                   "/users"                   {""                :user-list
                                               "/create"         :user-create
                                               ["/" :id]         :user-info
                                               ["/" :id "/edit"] :user-edit}}])

(defn- path
  [routes prefix handler params query]
  (if (some? query)
    (str prefix (b/unmatch-pair routes {:handler handler
                                        :params  params}) "?" (map->query query))
    (str prefix (b/unmatch-pair routes {:handler handler
                                        :params  params}))))

(defn path-for-frontend
  ([handler] (path-for-frontend handler {} nil))
  ([handler params] (path-for-frontend handler params nil))
  ([handler params query] (path frontend "#" handler params query)))

(defn path-for-backend
  ([handler] (path-for-backend handler {} nil))
  ([handler params] (path-for-backend handler params nil))
  ([handler params query] (str/replace (path backend "" handler params query) #"^/" "")))
