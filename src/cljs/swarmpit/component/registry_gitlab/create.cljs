(ns swarmpit.component.registry-gitlab.create
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
             [:span "Please enter your gitlab username and personal access token with API scope. See "]
             [:a {:href   "https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html"
                  :target "_blank"} "Personal Access Tokens"]
             [:br]
             [:span "If you're using self hosted Gitlab enter corresponding registry & gitlab url."]]))

(defn- form-hosted [value]
  (comp/switch
    {:name     "hosted"
     :label    "Hosted"
     :color    "primary"
     :value    (str value)
     :checked  value
     :onChange (fn [e]
                 (let [hosted (-> e .-target .-checked)]
                   (state/update-value [:hosted] hosted state/form-value-cursor)
                   (when (false? hosted)
                     (do
                       (state/update-value [:url] "https://registry.gitlab.com" state/form-value-cursor)
                       (state/update-value [:gitlabUrl] "https://gitlab.com" state/form-value-cursor)))))}))

(defn- form-gitlab-url [url]
  (comp/text-field
    {:label           "Gitlab url"
     :fullWidth       true
     :key             "gurl"
     :variant         "outlined"
     :defaultValue    url
     :required        true
     :margin          "normal"
     :placeholder     "e.g. https://my.gitlab.com"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:gitlabUrl] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-url [url]
  (comp/text-field
    {:label           "Registry url"
     :fullWidth       true
     :key             "url"
     :variant         "outlined"
     :defaultValue    url
     :required        true
     :margin          "normal"
     :placeholder     "e.g. https://my.registry.com"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:url] (-> % .-target .-value) state/form-value-cursor)}))

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
    (routes/path-for-backend :registries {:registryType :gitlab})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :gitlab
                                                                 :id           (:id response)})))
                   (message/info
                     (str "Gitlab registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Gitlab registry creation failed. " (:error response))))}))

(defn init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :showToken   false} state/form-state-cursor))

(defn init-form-value
  []
  (state/set-value {:hosted    false
                    :gitlabUrl "https://gitlab.com"
                    :url       "https://registry.gitlab.com"
                    :public    false
                    :username  ""
                    :token     ""} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [username token hosted url gitlabUrl]} (state/react state/form-value-cursor)
        {:keys [showToken]} (state/react state/form-state-cursor)]
    (state/update-value [:valid?] (not
                                    (or
                                      (str/blank? username)
                                      (str/blank? token)
                                      (str/blank? gitlabUrl)
                                      (str/blank? url))) state/form-state-cursor)
    (html
      [:div
       (comp/form-control
         {:component "fieldset"}
         (comp/form-group
           {}
           (comp/form-control-label
             {:control (form-hosted hosted)
              :label   "Hosted Gitlab"})))
       (when hosted
         (html
           [:div
            (form-gitlab-url gitlabUrl)
            (form-url url)]))
       (form-username username)
       (form-token token showToken)])))
