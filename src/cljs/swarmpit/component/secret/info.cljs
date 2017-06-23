(ns swarmpit.component.secret.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-secret-info-msg
  [id]
  (str "Secret " id " has been removed."))

(defn- delete-secret-error-msg
  [error]
  (str "Secret removing failed. Reason: " error))

(defn- delete-secret-handler
  [secret-id]
  (ajax/DELETE (routes/path-for-backend :secret-delete {:id secret-id})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (dispatch!
                                   (routes/path-for-frontend :secret-list))
                                 (message/mount!
                                   (delete-secret-info-msg secret-id)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")]
                                   (message/mount!
                                     (delete-secret-error-msg error) true)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/secrets
                      (:secretName item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-secret-handler (:id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:secretName item))
     (comp/form-item "CREATED" (:createdAt item))
     (comp/form-item "UPDATED" (:updatedAt item))]]])