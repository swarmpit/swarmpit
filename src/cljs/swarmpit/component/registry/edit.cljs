(ns swarmpit.component.registry.edit
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :registry :form])

(defn- form-public [value]
  (comp/form-comp
    "PUBLIC"
    (comp/form-checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v cursor))})))

(defn- update-registry-handler
  [registry-id]
  (handler/post
    (routes/path-for-backend :registry-update {:id registry-id})
    {:params     (state/get-value cursor)
     :on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-info {:id registry-id}))
                   (message/info
                     (str "Registry " registry-id " has been updated.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Registry update failed. Reason: " (:error response))))}))

(defn- init-state
  [registry]
  (state/set-value (select-keys registry [:public]) cursor))

(def init-state-mixin
  (mixin/init
    (fn [registry]
      (init-state registry))))

(rum/defc form < rum/reactive
                 init-state-mixin [registry]
  (let [{:keys [public]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/registries
                        (:name registry))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(update-registry-handler (:_id registry))
            :label      "Save"
            :primary    true}))
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:href  (routes/path-for-frontend :registry-info {:id (:_id registry)})
            :label "Back"}))]]
     [:div.form-edit
      (comp/form
        nil
        (form-public public))]]))
