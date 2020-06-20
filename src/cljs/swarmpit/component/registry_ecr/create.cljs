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
             [:span "In order to access ECR, amazon IAM user with given policy must be created first. See "]
             [:a {:href   "https://docs.aws.amazon.com/AmazonECR/latest/userguide/ECR_IAM_user_policies.html"
                  :target "_blank"} "IAM ECR User Guide"]
             [:br]
             [:span "We strongly recommned you to set "]
             [:a {:href   "https://docs.aws.amazon.com/AmazonECR/latest/userguide/ecr_managed_policies.html#AmazonEC2ContainerRegistryReadOnly"
                  :target "_blank"} "READ-ONLY Access"]]))

(def supported-region
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

(defn- form-region [region]
  (comp/text-field
    {:fullWidth       true
     :label           "Region"
     :key             "region"
     :select          true
     :margin          "normal"
     :required        true
     :value           region
     :variant         "outlined"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-value [:region] (-> % .-target .-value) state/form-value-cursor)}
    (map #(comp/menu-item
            {:key   %
             :value %} %) supported-region)))

(defn- form-user [user]
  (comp/text-field
    {:label           "User"
     :fullWidth       true
     :key             "user"
     :variant         "outlined"
     :defaultValue    user
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:user] (-> % .-target .-value) state/form-value-cursor)}))

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
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-key? :showKey)}}))

(defn create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registries {:registryType :ecr})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :ecr
                                                                 :id           (:id response)})))
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
                    :user        "swarmpit-ecr"
                    :accessKeyId ""
                    :accessKey   ""
                    :public      false} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(rum/defc form < rum/reactive [_]
  (let [{:keys [region user accessKeyId accessKey]} (state/react state/form-value-cursor)
        {:keys [showKey]} (state/react state/form-state-cursor)]
    (state/update-value [:valid?] (not
                                    (or
                                      (str/blank? user)
                                      (str/blank? accessKeyId)
                                      (str/blank? accessKey))) state/form-state-cursor)
    (html
      [:div
       (form-region region)
       (form-user user)
       (form-access-key-id accessKeyId)
       (form-access-key accessKey showKey)])))
