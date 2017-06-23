(ns swarmpit.component.registry.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-registry-info-msg
  [id]
  (str "Registry " id " has been removed."))

(defn- delete-registry-error-msg
  [error]
  (str "Registry removing failed. Reason: " error))

(defn- delete-registry-handler
  [registry-id]
  (ajax/DELETE (routes/path-for-backend :registry-delete {:id registry-id})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (dispatch!
                                   (routes/path-for-frontend :registry-list))
                                 (message/mount!
                                   (delete-registry-info-msg registry-id)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")]
                                   (message/mount!
                                     (delete-registry-error-msg error) true)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/registries
                      (:name item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-registry-handler (:_id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:_id item))
     (comp/form-item "NAME" (:name item))
     (comp/form-item "URL" (:url item))
     (comp/form-item "AUTHENTICATION" (if (:withAuth item)
                                        "yes"
                                        "no"))
     (if (:withAuth item)
       [:div
        (comp/form-item "USERNAME" (:username item))
        (comp/form-item "PASSWORD" (:password item))])]]])
