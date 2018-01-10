(ns swarmpit.component.user.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
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

(defonce valid? (atom false))

(defonce loading? (atom false))

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

(defn- user-handler
  [user-id]
  (handler/get
    (routes/path-for-backend :user {:id user-id})
    {:state      loading?
     :on-success (fn [response]
                   (state/set-value response cursor))}))

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

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (user-handler id))))

(rum/defc form-edit < rum/reactive
                      rum/static [{:keys [role email] :as user}]
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
          :disabled   (not (rum/react valid?))
          :primary    true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :user-info {:id (:_id user)})
          :label "Back"}))]]
   [:div.form-edit
    (form/form
      {:onValid   #(reset! valid? true)
       :onInvalid #(reset! valid? false)}
      (form-role role)
      (form-email email))]])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [user (state/react cursor)]
    (progress/form
      (rum/react loading?)
      (form-edit user))))