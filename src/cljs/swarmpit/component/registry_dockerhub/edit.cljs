(ns swarmpit.component.registry-dockerhub.edit
  (:require [material.icon :as icon]
            [material.components :as comp]
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

(defn- form-username [value]
  (comp/text-field
    {:label           "Username"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :disabled        true
     :margin          "normal"
     :InputLabelProps {:shrink true}}))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "New Password"
     :variant         "outlined"
     :fullWidth       true
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :margin          "normal"
     :InputProps      {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-public [value]
  (comp/switch
    {:checked  value
     :value    (str value)
     :color    "primary"
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           user-id
                                        :registryType :dockerhub})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-dockerhub-handler
  [user-id]
  (ajax/post
    (routes/path-for-backend :registry {:id           user-id
                                        :registryType :dockerhub})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :dockerhub
                                                                 :id           user-id})))
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
        (comp/container
          {:maxWidth  "sm"
           :className "Swarmpit-container"}
          (comp/card
            {:className "Swarmpit-form-card Swarmpit-fcard"}
            (comp/box
              {:className "Swarmpit-fcard-header"}
              (comp/typography
                {:className "Swarmpit-fcard-header-title"
                 :variant   "h6"
                 :component "div"}
                "Edit registry"))
            (comp/card-content
              {:className "Swarmpit-fcard-content"}
              (comp/typography
                {:variant   "body2"
                 :className "Swarmpit-fcard-message"}
                "Update Dockerhub account settings")
              (form-username username)
              (form-password password showPassword)
              (comp/form-control
                {:component "fieldset"
                 :key       "role-f"
                 :margin    "normal"}
                (comp/form-label
                  {:key "rolel"} "Make account Public")
                (comp/form-helper-text
                  {} "Means that anyone can search & deploy private repositories from this account")
                (comp/form-control-label
                  {:control (form-public public)}))
              (comp/box
                {:className "Swarmpit-form-buttons"}
                (composite/progress-button
                  "Save"
                  #(update-dockerhub-handler _id)
                  processing?)))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
