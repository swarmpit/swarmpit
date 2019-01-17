(ns swarmpit.component.registry-v2.create
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

(defn- form-name [name url]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :key             "name"
     :variant         "outlined"
     :defaultValue    name
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        (fn [e]
                        (state/update-value [:name] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? (-> e .-target .-value))
                                                          (str/blank? url))) state/form-state-cursor))}))

(defn- form-url [name url]
  (comp/text-field
    {:label           "Url"
     :fullWidth       true
     :type            "url"
     :key             "url"
     :variant         "outlined"
     :defaultValue    url
     :required        true
     :margin          "normal"
     :placeholder     "e.g. https://my.registry.io"
     :InputLabelProps {:shrink true}
     :onChange        (fn [e]
                        (state/update-value [:url] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? (-> e .-target .-value))
                                                          (str/blank? name))) state/form-state-cursor))}))

(defn- form-auth [value]
  (comp/switch
    {:name     "authentication"
     :label    "Authentication"
     :color    "primary"
     :value    (str value)
     :checked  value
     :onChange #(state/update-value [:withAuth] (-> % .-target .-checked) state/form-value-cursor)}))

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
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "Password"
     :variant         "outlined"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registry-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :reg-v2-info (select-keys response [:id]))))
                   (message/info
                     (str "Registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry creation failed. " (:error response))))}))

(defn init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn init-form-value
  []
  (state/set-value {:name     ""
                    :url      ""
                    :public   false
                    :withAuth false
                    :username ""
                    :password ""} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [name url withAuth username password]} (state/react state/form-value-cursor)
        {:keys [showPassword]} (state/react state/form-state-cursor)]
    (html
      [:div
       (form-name name url)
       (form-url name url)
       (comp/form-control
         {:component "fieldset"
          :key       "rcccigc"}
         (comp/form-group
           {:key "rcccigcg"}
           (comp/form-control-label
             {:control (form-auth withAuth)
              :key     "rcccigcga"
              :label   "Secured"})))
       (when withAuth
         (html
           [:div {:key "rcccigaut"}
            (form-username username)
            (form-password password showPassword)]))])))
