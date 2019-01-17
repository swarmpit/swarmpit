(ns swarmpit.component.registry-v2.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
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
                     (routes/path-for-frontend :distribution-list))
                   (message/info
                     (str "Registry " registry-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry removing failed. " (:error response))))}))

(defn- update-registry-handler
  [registry-id delta]
  (let [params (state/get-value state/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :registry-update {:id registry-id})
      {:params     delta
       :on-success (fn [_]
                     (state/set-value (merge params delta) state/form-value-cursor)
                     (message/info
                       (str "Registry " registry-id " has been updated.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Registry update failed. " (:error response))))})))

(defn form-actions
  [id public]
  [(if (true? public)
     {:onClick #(update-registry-handler id {:public false})
      :icon    (icon/lock {})
      :name    "Hide account"}
     {:onClick #(update-registry-handler id {:public true})
      :icon    (icon/share {})
      :name    "Share account"})
   {:onClick #(delete-registry-handler id)
    :icon    (comp/svg icon/trash-path)
    :name    "Delete registry"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (registry-handler id))))

(rum/defc form-info < rum/static [{:keys [_id name url username public withAuth]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   16}
          (comp/grid
            {:item true
             :key  "rgg"
             :xs   12
             :sm   6}
            (comp/card
              {:className "Swarmpit-form-card"
               :key       "rgc"}
              (comp/card-header
                {:title     name
                 :className "Swarmpit-form-card-header"
                 :key       "rgch"
                 :subheader url
                 :action    (common/actions-menu
                              (form-actions _id public)
                              :registryGeneralMenuAnchor
                              :registryGeneralMenuOpened)})
              (comp/card-content
                {:key "rgcc"}
                (html
                  [:div {:key "rgccd"}
                   (when withAuth
                     [:span "Authenticated with user " [:b username] "."])
                   [:br]
                   [:span "Account is " [:b (if public "public." "private.")]]]))
              (comp/card-content
                {:key "rgccl"}
                (form/item-labels
                  [(label/grey "Custom v2")]))
              (comp/divider
                {:key "rgd"})
              (comp/card-content
                {:style {:paddingBottom "16px"}
                 :key   "rgccf"}
                (form/item-id _id)))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info registry))))
