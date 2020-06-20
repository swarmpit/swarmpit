(ns swarmpit.component.registry-ecr.edit
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

(def supported-roles
  ["us-east-2"
   "us-east-1"
   "us-west-2"
   "us-west-1"
   "ap-east-1"
   "ap-south-1"
   "ap-northeast-3"
   "ap-northeast-2"
   "ap-northeast-1"
   "ap-southeast-2"
   "ap-southeast-1"
   "ca-central-1"
   "cn-north-1"
   "cn-northwest-1"
   "eu-central-1"
   "eu-west-3"
   "eu-west-2"
   "eu-west-1"
   "eu-north-1"
   "me-south-1"
   "sa-east-1"
   "us-gov-east-1"
   "us-gov-west-1"])

(defn- form-region [value]
  (comp/text-field
    {:fullWidth       true
     :label           "Region"
     :key             "region"
     :select          true
     :margin          "normal"
     :required        true
     :value           value
     :variant         "outlined"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-value [:region] (-> % .-target .-value) state/form-value-cursor)}
    (map #(comp/menu-item
            {:key   %
             :value %} %) supported-roles)))

(defn- form-user [value]
  (comp/text-field
    {:label           "User"
     :fullWidth       true
     :name            "user"
     :key             "user"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :disabled        true
     :margin          "normal"
     :InputLabelProps {:shrink true}}))

(defn- form-access-key-id [access-key-id]
  (comp/text-field
    {:label           "Access Key Id"
     :fullWidth       true
     :name            "access-key-id"
     :key             "access-key-id"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    access-key-id
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:accessKeyId] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-access-key [access-key show-key?]
  (comp/text-field
    {:label           "Secret Access Key"
     :variant         "outlined"
     :key             "access-key"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-key?
                        "text"
                        "password")
     :defaultValue    access-key
     :onChange        #(state/update-value [:accessKey] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:endAdornment (common/show-password-adornment show-key?)}}))

(defn- form-public [value]
  (comp/switch
    {:checked  value
     :value    (str value)
     :color    "primary"
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- ecr-handler
  [ecr-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           ecr-id
                                        :registryType :ecr})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-ecr-handler
  [ecr-id]
  (ajax/post
    (routes/path-for-backend :registry {:id           ecr-id
                                        :registryType :ecr})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :ecr
                                                                 :id           ecr-id})))
                   (message/info
                     (str "Registry " ecr-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry update failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading?    true
                    :processing? false
                    :showKey     false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (ecr-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id user public region accessKey accessKeyId]}
                                  {:keys [processing? showKey]}]
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
                "Update Amazon ECR account settings")
              (form-user user)
              (form-region region)
              (form-access-key-id accessKeyId)
              (form-access-key accessKey showKey)
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
                  #(update-ecr-handler _id)
                  processing?)))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
