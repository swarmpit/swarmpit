(ns swarmpit.component.registry.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- registry-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry {:id registry-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-registry-handler
  [registry-id]
  (ajax/delete
    (routes/path-for-backend :registry-delete {:id registry-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Registry " registry-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry removing failed. Reason: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (registry-handler id))))

(rum/defc form-info < rum/static [{:keys [_id name] :as registry}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/registries name)]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :registry-edit {:id _id})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-registry-handler _id)
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:_id registry))
     (form/item "NAME" (:name registry))
     (form/item "URL" (:url registry))
     (form/item "PUBLIC" (if (:public registry)
                           "yes"
                           "no"))
     (form/item "AUTHENTICATION" (if (:withAuth registry)
                                   "yes"
                                   "no"))
     (when (:withAuth registry)
       [:div
        (form/item "USERNAME" (:username registry))])]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info registry))))
