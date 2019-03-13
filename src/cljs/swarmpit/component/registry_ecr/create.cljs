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
  (comp/form-control
    {:component "fieldset"
     :key       "region-f"
     :margin    "normal"
     :style     {:width "200px"}}
    (comp/form-label
      {:key "regionl"} "Region")
    (comp/radio-group
      {:name     "region"
       :key      "region-rg"
       :value    value
       :onChange #(state/update-value [:region] (-> % .-target .-value) state/form-value-cursor)}
      (map #(comp/form-control-label
              {:control (comp/radio
                          {:name  (str % "-role")
                           :color "primary"
                           :key   (str % "-role")})
               :key     %
               :value   %
               :label   %}) supported-roles))))

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
                        (state/update-value [:access-key-id] (-> e .-target .-value) state/form-value-cursor)
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
                        (state/update-value [:access-key] (-> e .-target .-value) state/form-value-cursor)
                        (state/update-value [:valid?] (not
                                                        (or
                                                          (str/blank? access-key-id)
                                                          (str/blank? (-> e .-target .-value)))) state/form-state-cursor))
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-key?)}}))

(defn add-user-handler
  []
  (ajax/post
    (routes/path-for-backend :ecr-user-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]

                   (message/info
                     (str "User " (:id response) " has been added.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User cannot be added. " (:error response))))}))

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

(rum/defc form < rum/reactive [_]
  (let [{:keys [region access-key-id access-key]} (state/react state/form-value-cursor)
        {:keys [showKey]} (state/react state/form-state-cursor)]
    (html
      [:div
       (form-region region)
       (form-access-key-id access-key-id access-key)
       (form-access-key access-key access-key-id showKey)])))
