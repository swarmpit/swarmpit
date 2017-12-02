(ns swarmpit.component.registry.info
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-registry-handler
  [registry-id]
  (handler/delete
    (routes/path-for-backend :registry-delete {:id registry-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Registry " registry-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Registry removing failed. Reason: " (:error response))))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/registries
                 (:name item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :registry-edit {:id (:_id item)})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-registry-handler (:_id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:_id item))
     (form/item "NAME" (:name item))
     (form/item "URL" (:url item))
     (form/item "PUBLIC" (if (:public item)
                                "yes"
                                "no"))
     (form/item "AUTHENTICATION" (if (:withAuth item)
                                        "yes"
                                        "no"))
     (if (:withAuth item)
       [:div
        (form/item "USERNAME" (:username item))])]]])
