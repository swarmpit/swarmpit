(ns swarmpit.routes)

(def backend
  ["" {"/"               {:get :index}
       "/login"          {:post :login}
       "/registries/"    {:get {"sum" :registries-sum}}
       "/services"       {:get  :services
                          :post :service-create}
       "/services/"      {:get    {[:id] :service}
                          :delete {[:id] :service-delete}
                          :post   {[:id] :service-update}}
       "/networks"       {:get  :networks
                          :post :network-create}
       "/networks/"      {:get    {[:id] :network}
                          :delete {[:id] :network-delete}}
       "/nodes"          {:get :nodes}
       "/nodes/"         {:get {[:id] :node}}
       "/tasks"          {:get :tasks}
       "/tasks/"         {:get {[:id] :task}}
       "/v1/registries/" {:get {[:registryName "/repo"] {""      :v1-repositories
                                                         "/tags" :v1-repository-tags}}}
       "/v2/registries/" {:get {[:registryName "/repo"] {""      :v2-repositories
                                                         "/tags" :v2-repository-tags}}}
       "/admin/"         {"users"       {:get  :users
                                         :post :user-create}
                          "users/"      {:get {[:id] :handler/user}}
                          "registries"  {:get  :registries
                                         :post :registry-create}
                          "registries/" {:get {[:id] :registry}}}}])

(def frontend ["" {"/"           :index
                   "/login"      :login
                   "/error"      :error
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
                   "/users"      {""        :user-list
                                  "/create" :user-create
                                  ["/" :id] :user-info}}])
