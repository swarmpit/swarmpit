(ns swarmpit.component.user.edit
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

(def cursor [:page :user :form])

(defn- form-role [value]
  (form/comp
    "ROLE"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:role] v cursor))}
      (comp/menu-item
        {:key         "fru"
         :value       "admin"
         :primaryText "admin"})
      (comp/menu-item
        {:key         "fra"
         :value       "user"
         :primaryText "user"}))))

(defn- form-email [value]
  (form/comp
    "EMAIL"
    (comp/vtext-field
      {:name            "email"
       :key             "email"
       :required        true
       :validations     "isEmail"
       :validationError "Please provide a valid Email"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:email] v cursor))})))

(defn- update-user-handler
  [user-id]
  (handler/post
    (routes/path-for-backend :user-update {:id user-id})
    {:params     (state/get-value cursor)
     :on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :user-info {:id user-id}))
                   (message/info
                     (str "User " user-id " has been updated.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User update failed. Reason: " (:error response))))}))

(defn- init-state
  [user]
  (state/set-value {:email   (:email user)
                    :role    (:role user)
                    :isValid true} cursor))

(def init-state-mixin
  (mixin/init
    (fn [user]
      (init-state user))))

(rum/defc form < rum/reactive
                 init-state-mixin [user]
  (let [{:keys [role
                email
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/users
                   (:username user))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(update-user-handler (:_id user))
            :label      "Save"
            :disabled   (not isValid)
            :primary    true}))
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:href  (routes/path-for-frontend :user-info {:id (:_id user)})
            :label "Back"}))]]
     [:div.form-edit
      (form/form
        {:onValid   #(state/update-value [:isValid] true cursor)
         :onInvalid #(state/update-value [:isValid] false cursor)}
        (form-role role)
        (form-email email))]]))