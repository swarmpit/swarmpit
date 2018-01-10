(ns swarmpit.component.registry.edit
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

(defonce loading? (atom false))

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v cursor))})))

(defn- registry-handler
  [registry-id]
  (handler/get
    (routes/path-for-backend :registry {:id registry-id})
    {:state      loading?
     :on-success (fn [response]
                   (state/set-value response cursor))}))

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

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (registry-handler id))))

(rum/defc form-edit < rum/static [registry]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/registries
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
    (form/form
      nil
      (form-public (:public registry)))]])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [registry (state/react cursor)]
    (progress/form
      (rum/react loading?)
      (form-edit registry))))
