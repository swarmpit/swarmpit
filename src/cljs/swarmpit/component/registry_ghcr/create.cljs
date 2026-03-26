(ns swarmpit.component.registry-ghcr.create
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
             [:span "Please enter your GitHub username and personal access token with "]
             [:code "read:packages"]
             [:span " scope. See "]
             [:a {:href   "https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry"
                  :target "_blank"} "GitHub Container Registry docs"]]))

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
    (routes/path-for-backend :registries {:registryType :ghcr})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :ghcr
                                                                 :id           (:id response)})))
                   (message/info
                     (str "GitHub registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "GitHub registry creation failed. " (:error response))))}))

(defn init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :showToken   false} state/form-state-cursor))

(defn init-form-value
  []
  (state/set-value {:url      "https://ghcr.io"
                    :public   false
                    :username ""
                    :token    ""} state/form-value-cursor))

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
