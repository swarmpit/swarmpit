(ns swarmpit.routes
  (:require [bidi.bidi :as b]
            [cemerick.url :refer [map->query]]))

(def backend
  ["" {"/"            {:get :index}
       "/login"       {:post :login}
       "/password"    {:post :password}
       "/registries/" {:get {"list"              :registries-list
                             [:registry "/repo"] {""      :repositories
                                                  "/tags" :repository-tags}}}
       "/dockerhub/"  {:get {"users"         {"/list" :dockerhub-users-list}
                             "repo"          {""      :dockerhub-repo
                                              "/tags" :dockerhub-tags}
                             [:user "/repo"] :dockerhub-user-repo}}
       "/services"    {:get  :services
                       :post :service-create}
       "/services/"   {:get    {[:id] {""          :service
                                       "/networks" :service-networks
                                       "/tasks"    :service-tasks}}
                       :delete {[:id] :service-delete}
                       :post   {[:id] :service-update}}
       "/networks"    {:get  :networks
                       :post :network-create}
       "/networks/"   {:get    {[:id] :network}
                       :delete {[:id] :network-delete}}
       "/volumes"     {:get  :volumes
                       :post :volume-create}
       "/volumes/"    {:get    {[:name] :volume}
                       :delete {[:name] :volume-delete}}
       "/secrets"     {:get  :secrets
                       :post :secret-create}
       "/secrets/"    {:get    {[:id] :secret}
                       :delete {[:id] :secret-delete}
                       :post   {[:id] :secret-update}}
       "/nodes"       {:get :nodes}
       "/nodes/"      {:get {"placement" :placement
                             [:id]       :node}}
       "/tasks"       {:get :tasks}
       "/tasks/"      {:get {[:id] :task}}
       "/admin/"      {"users"            {:get  :users
                                           :post :user-create}
                       "users/"           {:get    {[:id] :user}
                                           :delete {[:id] :user-delete}}
                       "dockerhub/users"  {:get  :dockerhub-users
                                           :post :dockerhub-user-create}
                       "dockerhub/users/" {:get    {[:id] :dockerhub-user}
                                           :delete {[:id] :dockerhub-user-delete}}
                       "registries"       {:get  :registries
                                           :post :registry-create}
                       "registries/"      {:get    {[:id] :registry}
                                           :delete {[:id] :registry-delete}}}}])

(def frontend ["" {"/"           :index
                   "/login"      :login
                   "/password"   :password
                   "/services"   {""                :service-list
                                  "/create/wizard"  {"/image"  :service-create-image
                                                     "/config" :service-create-config}
                                  ["/" :id]         :service-info
                                  ["/" :id "/edit"] :service-edit}
                   "/networks"   {""        :network-list
                                  "/create" :network-create
                                  ["/" :id] :network-info}
                   "/volumes"    {""          :volume-list
                                  "/create"   :volume-create
                                  ["/" :name] :volume-info}
                   "/secrets"    {""        :secret-list
                                  "/create" :secret-create
                                  ["/" :id] :secret-info}
                   "/nodes"      {"" :node-list}
                   "/tasks"      {""        :task-list
                                  ["/" :id] :task-info}
                   "/registries" {""        :registry-list
                                  "/add"    :registry-create
                                  ["/" :id] :registry-info}
                   "/dockerhub"  {""        :dockerhub-user-list
                                  "/add"    :dockerhub-user-create
                                  ["/" :id] :dockerhub-user-info}
                   "/users"      {""        :user-list
                                  "/create" :user-create
                                  ["/" :id] :user-info}}])

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
