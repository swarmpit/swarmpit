(ns swarmpit.component.registry-ecr.create
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
             [:span "In order to access registry amazon IAM account must be created first. See "]
             [:a {:href   "https://aws.amazon.com/iam"
                  :target "_blank"} "https://aws.amazon.com/iam"]]))

(def supported-roles
  ["us-east-2"
   "us-east-1"
   "us-west-2"
   "us-west-1"
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

(defn- form-access-key-id [access-key-id access-key]
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
     :onChange        (fn [e]
                        (state/update-value [:accessKeyId] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? (-> e .-target .-value))
                                                          (str/blank? access-key))) state/form-state-cursor))}))

(defn- form-access-key [access-key access-key-id show-key?]
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
     :onChange        (fn [e]
                        (state/update-value [:accessKey] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? access-key-id)
                                                          (str/blank? (-> e .-target .-value)))) state/form-state-cursor))
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-key? :showKey)}}))

(defn- create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :ecr-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :reg-ecr-info (select-keys response [:id]))))
                   (message/info
                     (str "Amazon ECR " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Amazon ECR creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :showKey     false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:region      "eu-west-1"
                    :accessKeyId ""
                    :accessKey   ""
                    :public      false} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [region accessKeyId accessKey]} (state/react state/form-value-cursor)
        {:keys [showKey]} (state/react state/form-state-cursor)]
    (html
      [:div
       (form-region region)
       (form-access-key-id accessKeyId accessKey)
       (form-access-key accessKey accessKeyId showKey)])))
