(ns swarmpit.component.iam.aws
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.common :as common]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

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

(defn- form-region [local-state]
  (comp/text-field
    {:fullWidth       true
     :label           "Region"
     :key             "region"
     :select          true
     :margin          "normal"
     :required        true
     :value           (:region @local-state)
     :variant         "outlined"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        (fn [event]
                        (swap! local-state assoc :region (-> event .-target .-value)))}
    (map #(comp/menu-item
            {:key   %
             :value %} %) supported-roles)))

(defn- form-access-key-id [local-state]
  (comp/text-field
    {:label           "Access Key Id"
     :fullWidth       true
     :name            "access-key-id"
     :key             "access-key-id"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    (:access-key-id @local-state)
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        (fn [e]
                        (swap! local-state assoc :access-key-id (-> e .-target .-value)))}))

(defn- form-access-key [local-state]
  (comp/text-field
    {:label           "Secret Access Key"
     :variant         "outlined"
     :key             "access-key"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if (:show-key @local-state)
                        "text"
                        "password")
     :defaultValue    (:access-key @local-state)
     :onChange        (fn [e]
                        (swap! local-state assoc :access-key (-> e .-target .-value)))
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment (:show-key @local-state))}}))

(defn add-account-handler
  []
  (ajax/post
    (routes/path-for-backend :aws-account-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :reg-ecr-info (select-keys response [:id]))))
                   (message/info
                     (str "IAM user " (:id response) " has been added.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "IAM user cannot be added. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :showKey     false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:region        "eu-west-1"
                    :access-key-id ""
                    :access-key    ""
                    :public        false} state/form-value-cursor))

(defn reset-form
  []
  (init-form-state)
  (init-form-value))

(def render-metadata
  {:primary   (fn [item] (:username item))
   :secondary (fn [item] (:secret-access-key item))})

(rum/defc form-list < rum/static []
  (list/list
    render-metadata
    []
    nil))

(rum/defcs form-new < (rum/local {:region        "eu-west-1"
                                  :access-key-id ""
                                  :access-key    ""
                                  :showKey       false} ::aws) [state]
  (let [local-state (::aws state)]
    [:div
     (form-region local-state)
     (form-access-key-id local-state)
     (form-access-key local-state)]))

(rum/defc form < rum/static []
  (html
    [:div
     (dialog/form-dialog
       #(add-account-handler)
       (form-new)
       "AWS IAM Account")
     (form/subsection
       "AWS Accounts"
       (comp/button
         {:color   "primary"
          :onClick #(state/update-value [:open] true dialog/dialog-cursor)}
         (comp/svg icon/add-small-path) "Add account"))
     (form-list)]))

