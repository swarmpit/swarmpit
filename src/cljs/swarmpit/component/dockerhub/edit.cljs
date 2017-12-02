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
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :dockerhub :form])

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v cursor))})))

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

(defn- init-state
  [user]
  (state/set-value (select-keys user [:public]) cursor))

(def init-state-mixin
  (mixin/init
    (fn [user]
      (init-state user))))

(rum/defc form < rum/reactive
                 init-state-mixin [user]
  (let [{:keys [public]} (state/react cursor)]
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
      (form/form nil (form-public public))]]))
