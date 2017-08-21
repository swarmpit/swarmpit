(ns swarmpit.routes
  (:require [bidi.bidi :as b]
            [cemerick.url :refer [map->query]]))

(def backend
  ["" {"/"              {:get :index}
       "/version"       {:get :version}
       "/login"         {:post :login}
       "/password"      {:post :password}
       "/distribution/" {"public"     {:get {"/repositories" :public-repositories
                                             "/tags"         :public-repository-tags
                                             "/ports"        :public-repository-ports}}
                         "dockerhub"  {""  {:get  :dockerhub-users
                                            :post :dockerhub-user-create}
                                       "/" {:get    {[:id "/repositories"] :dockerhub-repositories
                                                     [:id "/tags"]         :dockerhub-repository-tags
                                                     [:id "/ports"]        :dockerhub-repository-ports
                                                     [:id]                 :dockerhub-user}
                                            :delete {[:id] :dockerhub-user-delete}
                                            :post   {[:id] :dockerhub-user-update}}}
                         "registries" {""  {:get  :registries
                                            :post :registry-create}
                                       "/" {:get    {[:id "/repositories"] :registry-repositories
                                                     [:id "/tags"]         :registry-repository-tags
                                                     [:id "/ports"]        :registry-repository-ports
                                                     [:id]                 :registry}
                                            :delete {[:id] :registry-delete}
                                            :post   {[:id] :registry-update}}}}
       "/services"      {:get  :services
                         :post :service-create}
       "/services/"     {:get    {[:id] {""          :service
                                         "/logs"     :service-logs
                                         "/networks" :service-networks
                                         "/tasks"    :service-tasks}}
                         :delete {[:id] :service-delete}
                         :post   {[:id] {""          :service-update
                                         "/redeploy" :service-redeploy
                                         "/rollback" :service-rollback}}}
       "/networks"      {:get  :networks
                         :post :network-create}
       "/networks/"     {:get    {[:id] :network}
                         :delete {[:id] :network-delete}}
       "/volumes"       {:get  :volumes
                         :post :volume-create}
       "/volumes/"      {:get    {[:name] :volume}
                         :delete {[:name] :volume-delete}}
       "/secrets"       {:get  :secrets
                         :post :secret-create}
       "/secrets/"      {:get    {[:id] :secret}
                         :delete {[:id] :secret-delete}
                         :post   {[:id] :secret-update}}
       "/nodes"         {:get :nodes}
       "/nodes/"        {:get {"placement" :placement
                               [:id]       :node}}
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
                   "/password"                :password
                   "/services"                {""               :service-list
                                               "/create/wizard" {"/image"  :service-create-image
                                                                 "/config" :service-create-config}
                                               ["/" :id]        {""      :service-info
                                                                 "/edit" :service-edit
                                                                 "/log"  :service-log}}
                   "/stacks"                  {""        :stack-list
                                               "/create" :stack-create}
                   "/networks"                {""        :network-list
                                               "/create" :network-create
                                               ["/" :id] :network-info}
                   "/volumes"                 {""          :volume-list
                                               "/create"   :volume-create
                                               ["/" :name] :volume-info}
                   "/secrets"                 {""        :secret-list
                                               "/create" :secret-create
                                               ["/" :id] :secret-info}
                   "/nodes"                   {"" :node-list}
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
  ([handler params query] (path frontend "/#" handler params query)))

(defn path-for-backend
  ([handler] (path-for-backend handler {} nil))
  ([handler params] (path-for-backend handler params nil))
  ([handler params query] (path backend "" handler params query)))
