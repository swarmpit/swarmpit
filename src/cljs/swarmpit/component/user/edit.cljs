(ns swarmpit.component.user.edit
  (:require [material.components :as comp]
            [material.component.composite :as composite]
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

(defn- form-username [value]
  (comp/text-field
    {:label           "Username"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    value
     :required        true
     :disabled        true
     :InputLabelProps {:shrink true}}))

(defn- form-role [value]
  (comp/text-field
    {:fullWidth       true
     :label           "Role"
     :key             "role"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:role] (-> % .-target .-value) state/form-value-cursor)}
    (comp/menu-item
      {:key   "admin"
       :value "admin"} "admin")
    (comp/menu-item
      {:key   "user"
       :value "user"} "user")))

(defn- form-email [value]
  (comp/text-field
    {:label           "Email"
     :fullWidth       true
     :variant         "outlined"
     :key             "email"
     :defaultValue    value
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:email] (-> % .-target .-value) state/form-value-cursor)}))

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :user {:id user-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-user-handler
  [user-id]
  (ajax/post
    (routes/path-for-backend :user-update {:id user-id})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :user-info {:id user-id})))
                   (message/info
                     (str "User " user-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User update failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :loading?    true
                    :processing? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id username role email]}
                                  {:keys [processing? valid?]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:item true
           :xs   12
           :sm   6
           :md   4}
          (comp/card
            {:className "Swarmpit-form-card"
             :key       "uec"}
            (comp/card-header
              {:className "Swarmpit-form-card-header"
               :key       "uech"
               :title     "Edit User"})
            (comp/card-content
              {:key "uecc"}
              (comp/grid
                {:container true
                 :key       "ueccc"
                 :spacing   40}
                (comp/grid
                  {:item true
                   :key  "uecccig"
                   :xs   12}
                  (form-username username)
                  (form-role role)
                  (form-email email)))
              (html
                [:div {:class "Swarmpit-form-buttons"
                       :key   "ueccbtn"}
                 (composite/progress-button
                   "Save"
                   #(update-user-handler _id)
                   processing?)]))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        user (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit user state))))