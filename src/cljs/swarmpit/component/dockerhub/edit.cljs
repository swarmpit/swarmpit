(ns swarmpit.component.dockerhub.edit
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v cursor))})))

(defn- user-handler
  [user-id]
  (handler/get
    (routes/path-for-backend :dockerhub-user {:id user-id})
    {:on-success (fn [response]
                   (state/set-value response cursor))}))

(defn- update-user-handler
  [user-id]
  (handler/post
    (routes/path-for-backend :dockerhub-user-update {:id user-id})
    {:params     (state/get-value cursor)
     :on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :dockerhub-user-info {:id user-id}))
                   (message/info
                     (str "User " user-id " has been updated.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User update failed. Reason: " (:error response))))}))

(def mixin-init-state
  (mixin/init-state
    (fn [{:keys [id]}]
      (user-handler id))))

(rum/defc form-edit < rum/static [user]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/docker
                 (:username user))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(update-user-handler (:_id user))
          :label      "Save"
          :primary    true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :dockerhub-user-info {:id (:_id user)})
          :label "Back"}))]]
   [:div.form-edit
    (form/form
      nil
      (form-public (:public user)))]])

(rum/defc form < rum/reactive
                 mixin-init-state [_]
  (let [user (state/react cursor)]
    (progress/form
      (nil? user)
      (form-edit user))))
