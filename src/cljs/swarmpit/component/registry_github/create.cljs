(ns swarmpit.component.registry-github.create
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
             [:span "Please enter your github username and personal access token with API scope. See "]
             [:a {:href   "https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token"
                  :target "_blank"} "Personal Access Tokens"]]))

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

(defn- form-token [value show-token?]
  (comp/text-field
    {:label           "Personal Access Token"
     :variant         "outlined"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-token?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:token] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-token? :showToken)}}))

(defn create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registries {:registryType :github})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :github
                                                                 :id           (:id response)})))
                   (message/info
                     (str "Github registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Github registry creation failed. " (:error response))))}))

(defn init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :showToken   false} state/form-state-cursor))

(defn init-form-value
  []
  (state/set-value {:githubUrl "https://github.com"
                    :url       "https://docker.pkg.github.com"
                    :public    false
                    :username  ""
                    :token     ""} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [username token]} (state/react state/form-value-cursor)
        {:keys [showToken]} (state/react state/form-state-cursor)]
    (state/update-value [:valid?] (not
                                    (or
                                      (str/blank? username)
                                      (str/blank? token))) state/form-state-cursor)
    (html
      [:div
       (form-username username)
       (form-token token showToken)])))