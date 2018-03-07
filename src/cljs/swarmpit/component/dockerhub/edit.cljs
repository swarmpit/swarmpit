(ns swarmpit.component.dockerhub.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v state/form-value-cursor))})))

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :dockerhub-user {:id user-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-user-handler
  [user-id]
  (ajax/post
    (routes/path-for-backend :dockerhub-user-update {:id user-id})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :dockerhub-user-info {:id user-id})))
                   (message/info
                     (str "User " user-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User update failed. Reason: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading?    true
                    :processing? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id username public]}
                                  {:keys [processing?]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/docker username)]
    [:div.form-panel-right
     (comp/progress-button
       {:label      "Save"
        :primary    true
        :onTouchTap #(update-user-handler _id)} processing?)
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :dockerhub-user-info {:id _id})
          :label "Back"}))]]
   [:div.form-edit
    (form/form
      nil
      (form-public public))]])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        user (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit user state))))
