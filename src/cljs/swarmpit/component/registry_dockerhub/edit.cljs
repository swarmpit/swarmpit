(ns swarmpit.component.registry-dockerhub.edit
  (:require [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.common :as common]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "New Password"
     :variant         "outlined"
     :fullWidth       true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-public [value]
  (comp/checkbox
    {:checked  value
     :value    (str value)
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

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
                       (routes/path-for-frontend :reg-dockerhub-info {:id user-id})))
                   (message/info
                     (str "Dockerhub account " user-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Dockerhub account update failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading?     true
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id username public password]}
                                  {:keys [processing? showPassword]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/card
          {:className "Swarmpit-form-card"
           :style     {:maxWidth "400px"}
           :key       "dec"}
          (comp/card-header
            {:className "Swarmpit-form-card-header"
             :key       "dech"
             :title     (html [:span "Editing " [:span.Swarmpit-secondary-title username]])})
          (comp/card-content
            {:key "decc"}
            (form-password password showPassword)
            (comp/form-control
              {:component "fieldset"
               :key       "decccigc"}
              (comp/form-group
                {:key "decccigcg"}
                (comp/form-control-label
                  {:control (form-public public)
                   :key     "decccigcgp"
                   :label   "Share"})))
            (html
              [:div {:class "Swarmpit-form-buttons"
                     :key   "deccbtn"}
               (composite/progress-button
                 "Save"
                 #(update-user-handler _id)
                 processing?)])))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
