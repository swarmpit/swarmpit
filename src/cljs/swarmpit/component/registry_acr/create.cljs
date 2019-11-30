(ns swarmpit.component.registry-acr.create
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

(def text (html
            [:div
             [:span "In order to access ACR, azure IAM user with given permissions must be created first. See "]
             [:a {:href   "https://docs.microsoft.com/en-us/azure/container-registry/container-registry-auth-service-principal"
                  :target "_blank"} "ACR SP Auth"]
             [:br]
             [:span "We strongly recommned you to use "]
             [:a {:href   "https://raw.githubusercontent.com/swarmpit/tooling/master/azure/create-acr-sp.sh"
                  :target "_blank"} "READER role only"]]))

(defn- form-registry-name [name]
  (comp/text-field
    {:label           "Registry name"
     :fullWidth       true
     :key             "name"
     :variant         "outlined"
     :defaultValue    name
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:name] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-principal-name [name]
  (comp/text-field
    {:label           "Service Principal Name"
     :fullWidth       true
     :key             "name"
     :variant         "outlined"
     :defaultValue    name
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:spName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-principal-id [id]
  (comp/text-field
    {:label           "Service Principal ID"
     :fullWidth       true
     :key             "id"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    id
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:spId] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-principal-password [password show-password?]
  (comp/text-field
    {:label           "Service Principal Password"
     :variant         "outlined"
     :key             "password"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    password
     :onChange        #(state/update-value [:spPassword] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-password? :showPassword)}}))

(defn create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registries {:registryType :acr})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :acr
                                                                 :id           (:id response)})))
                   (message/info
                     (str "Azure ACR " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Azure ACR creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:name       ""
                    :spName     "swarmpit-acr"
                    :spId       ""
                    :spPassword ""
                    :public     false} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [name spName spId spPassword]} (state/react state/form-value-cursor)
        {:keys [showPassword]} (state/react state/form-state-cursor)]
    (state/update-value [:valid?] (not
                                    (or
                                      (str/blank? name)
                                      (str/blank? spName)
                                      (str/blank? spId)
                                      (str/blank? spPassword))) state/form-state-cursor)
    (html
      [:div
       (form-registry-name name)
       (form-principal-name spName)
       (form-principal-id spId)
       (form-principal-password spPassword showPassword)])))
