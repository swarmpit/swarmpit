(ns swarmpit.component.registry-dockerhub.create
  (:require [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.common :as common]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-username [username password]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    username
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        (fn [e]
                        (state/update-value [:username] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? (-> e .-target .-value))
                                                          (str/blank? password))) state/form-state-cursor))}))

(defn- form-password [username password show-password?]
  (comp/text-field
    {:label           "Password"
     :variant         "outlined"
     :key             "password"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    password
     :onChange        (fn [e]
                        (state/update-value [:password] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? username)
                                                          (str/blank? (-> e .-target .-value)))) state/form-state-cursor))
     :InputLabelProps {:shrink true}
     :InputProps      {:endAdornment (common/show-password-adornment show-password?)}}))

(defn add-user-handler
  []
  (ajax/post
    (routes/path-for-backend :dockerhub-user-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :reg-dockerhub-info (select-keys response [:id]))))
                   (message/info
                     (str "Dockerhub user " (:id response) " has been added.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Dockerhub user cannot be added. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:username ""
                    :password ""
                    :public   false} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [username password]} (state/react state/form-value-cursor)
        {:keys [showPassword]} (state/react state/form-state-cursor)]
    (html
      [:div
       (form-username username password)
       (form-password username password showPassword)])))
