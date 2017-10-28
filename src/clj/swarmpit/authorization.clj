(ns swarmpit.authorization
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [success error wrap-access-rules]]
            [swarmpit.handler :refer [resp-error]]
            [swarmpit.couchdb.client :as cc]))

(defn- authenticated-access
  [request]
  (if (authenticated? request)
    true
    (error {:code    401
            :message "Authentication failed"})))

(defn- any-access
  [_]
  true)

(defn- admin-access
  [{:keys [identity]}]
  (let [role (get-in identity [:usr :role])]
    (if (= "admin" role)
      true
      (error {:code    403
              :message "Unauthorized admin access"}))))

(defn- owner-access
  [{:keys [path-params identity]}]
  (let [user (get-in identity [:usr :username])
        entity (cc/get-doc (:id path-params))]
    (if (= (:owner entity) user)
      true
      (error {:code    403
              :message "Unauthorized owner access"}))))

(defn- distribution-access
  [{:keys [path-params identity]}]
  (let [user (get-in identity [:usr :username])
        entity (cc/get-doc (:id path-params))]
    (if (or (= (:owner entity) user)
            (:public entity))
      true
      (error {:code    403
              :message "Unauthorized distribution access"}))))

(defn- service-distribution-access
  [{:keys [json-params identity]}]
  (let [user (get-in identity [:usr :username])
        distribution-id (get-in json-params ["distribution" "id"])]
    (if (nil? distribution-id)
      true
      (let [entity (cc/get-doc distribution-id)]
        (if (or (= (:owner entity) user)
                (:public entity))
          true
          (error {:code    403
                  :message "Unauthorized distribution access"}))))))

(def rules [{:pattern #"^/admin/.*"
             :handler {:and [authenticated-access admin-access]}}
            {:pattern #"^/login$"
             :handler any-access}
            {:pattern #"^/events"
             :handler any-access}
            {:pattern #"^/version$"
             :handler any-access}
            {:pattern #"^/$"
             :handler any-access}
            {:pattern        #"^/distribution/registries/[a-zA-Z0-9]*/repositories$"
             :request-method :get
             :handler        {:and [authenticated-access distribution-access]}}
            {:pattern        #"^/distribution/registries/[a-zA-Z0-9]*/tags$"
             :request-method :get
             :handler        {:and [authenticated-access distribution-access]}}
            {:pattern        #"^/distribution/registries/[a-zA-Z0-9]*/ports$"
             :request-method :get
             :handler        {:and [authenticated-access distribution-access]}}
            {:pattern        #"^/distribution/registries/[a-zA-Z0-9]*$"
             :request-method #{:get :delete :post}
             :handler        {:and [authenticated-access owner-access]}}
            {:pattern        #"^/distribution/dockerhub/[a-zA-Z0-9]*/repositories$"
             :request-method :get
             :handler        {:and [authenticated-access distribution-access]}}
            {:pattern        #"^/distribution/dockerhub/[a-zA-Z0-9]*/tags$"
             :request-method :get
             :handler        {:and [authenticated-access distribution-access]}}
            {:pattern        #"^/distribution/dockerhub/[a-zA-Z0-9]*/ports$"
             :request-method :get
             :handler        {:and [authenticated-access distribution-access]}}
            {:pattern        #"^/distribution/dockerhub/[a-zA-Z0-9]*$"
             :request-method #{:get :delete :post}
             :handler        {:and [authenticated-access owner-access]}}
            {:pattern        #"^/services$"
             :request-method :post
             :handler        {:and [authenticated-access service-distribution-access]}}
            {:pattern #"^/.*"
             :handler {:and [authenticated-access]}}])

(defn- rules-error
  [_ val]
  (resp-error (:code val)
              (:message val)))

(defn wrap-authorization
  [handler]
  (wrap-access-rules handler {:rules    rules
                              :on-error rules-error}))
