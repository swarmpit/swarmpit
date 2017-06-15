(ns swarmpit.routes
  (:require [bidi.bidi :as b]
            [cemerick.url :refer [map->query]]))

(def backend
  ["" {"/"            {:get :index}
       "/login"       {:post :login}
       "/password"    {:post :password}
       "/registries/" {:get {"sum"                   :registries-sum
                             [:registryName "/repo"] {""      :repositories
                                                      "/tags" :repository-tags}}}
       "/dockerhub/"  {:get {"users"             {"/sum" :dockerhub-users-sum}
                             "repo"              {""      :dockerhub-repo
                                                  "/tags" :dockerhub-tags}
                             [:username "/repo"] :dockerhub-user-repo}}
       "/services"    {:get  :services
                       :post :service-create}
       "/services/"   {:get    {[:id] :service}
                       :delete {[:id] :service-delete}
                       :post   {[:id] :service-update}}
       "/networks"    {:get  :networks
                       :post :network-create}
       "/networks/"   {:get    {[:id] :network}
                       :delete {[:id] :network-delete}}
       "/nodes"       {:get :nodes}
       "/nodes/"      {:get {[:id] :node}}
       "/tasks"       {:get :tasks}
       "/tasks/"      {:get {[:id] :task}}
       "/admin/"      {"users"            {:get  :users
                                           :post :user-create}
                       "dockerhub/users"  {:get  :dockerhub-users
                                           :post :dockerhub-user-create}
                       "dockerhub/users/" {:get    {[:id] :dockerhub-user}
                                           :delete {[:id] :dockerhub-user-delete}}
                       "users/"           {:get    {[:id] :user}
                                           :delete {[:id] :user-delete}}
                       "registries"       {:get  :registries
                                           :post :registry-create}
                       "registries/"      {:get    {[:id] :registry}
                                           :delete {[:id] :registry-delete}}}}])

(def frontend ["" {"/"           :index
                   "/login"      :login
                   "/error"      :error
                   "/password"   :password
                   "/services"   {""                :service-list
                                  "/create/wizard"  {"/image"  :service-create-image
                                                     "/config" :service-create-config}
                                  ["/" :id]         :service-info
                                  ["/" :id "/edit"] :service-edit}
                   "/networks"   {""        :network-list
                                  "/create" :network-create
                                  ["/" :id] :network-info}
                   "/nodes"      {""        :node-list
                                  ["/" :id] :node-info}
                   "/tasks"      {""        :task-list
                                  ["/" :id] :task-info}
                   "/registries" {""        :registry-list
                                  "/create" :registry-create
                                  ["/" :id] :registry-info}
                   "/dockerhub"  {""        :dockerhub-user-list
                                  "/create" :dockerhub-user-create
                                  ["/" :id] :dockerhub-user-info}
                   "/users"      {""        :user-list
                                  "/create" :user-create
                                  ["/" :id] :user-info}}])

(defn path-for-frontend
  ([handler params query] (str "/#" (b/unmatch-pair frontend {:handler handler
                                                              :params  params})
                               "?" (map->query query)))
  ([handler params] (str "/#" (b/unmatch-pair frontend {:handler handler
                                                        :params  params})))
  ([handler] (str "/#" (b/unmatch-pair frontend {:handler handler
                                                 :params  {}}))))

(defn path-for-backend
  ([handler params query] (str (b/unmatch-pair backend {:handler handler
                                                        :params  params})
                               "?" (map->query query)))
  ([handler params] (b/unmatch-pair backend {:handler handler
                                             :params  params}))
  ([handler] (b/unmatch-pair backend {:handler handler
                                      :params  {}})))
