(ns swarmpit.component.registry.create
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.registry-v2.create :as v2]
            [swarmpit.component.registry-dockerhub.create :as dockerhub]))

(enable-console-print!)

(defonce step-index (atom 0))

(defonce registry (atom "dockerhub"))

(def steps ["Choose type"
            "Provide details"
            "Share with team"])

(defn last-step?
  [index]
  (= index (- (count steps) 1)))

(def registry-type-text
  "Specify registry account type you would like to use to authenticate
   your private repositories.")

(defn- registry-type-form [value]
  (comp/text-field
    {:fullWidth       true
     :key             "distrt"
     :label           "Registry account type"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        (fn [e]
                        (let [type (-> e .-target .-value)]
                          (reset! registry type)
                          (case type
                            "dockerhub" (dockerhub/reset-form)
                            "v2" (v2/reset-form))))}
    (comp/menu-item
      {:key   "dockerhub"
       :value "dockerhub"} "Dockerhub")
    (comp/menu-item
      {:key   "v2"
       :value "v2"} "Registry v2")))

(defn- registry-text [registry]
  (case registry
    "dockerhub" "Please enter your docker login credentials. All your
                 linked namespaces access will be granted."
    "v2" "Please enter your custom v2 registry name & address.
          If you are using secured registry provide account
          credentials as well."))

(defn- registry-form [registry route]
  (case registry
    "dockerhub" (dockerhub/form route)
    "v2" (v2/form route)))

(def registry-publish-text
  "By default only user that has created registry account can
   search & deploy private repositories. If you would like to share
   this access with other members of your team check the share button.")

(defn- registry-publish-form [value]
  (comp/form-control
    {:component "fieldset"
     :key       "fcp"}
    (comp/form-group
      {:key "fcpg"}
      (comp/form-control-label
        {:control (comp/checkbox
                    {:checked  value
                     :value    (str value)
                     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)})
         :key     "fcpbl"
         :label   "Share"}))))

(defn step-item
  ([index valid? text form]
   (step-item index valid? text form false))
  ([index valid? text form processing?]
   (print index valid? text)
   (comp/step
     {:key (str "step-" index)}
     (comp/step-label
       {:key   (str "step-label-" index)
        :style {:marginBottom "10px"}}
       (nth steps index))
     (comp/step-content
       {:key (str "step-content-" index)}
       (comp/typography
         {:key   (str "step-typo-" index)
          :style {:marginBottom "10px"}} text)
       (html
         [:div {:key (str "step-form-" index)} form])
       (html
         [:div {:key       (str "step-actions-" index)
                :className "Swarmpit-form-buttons"}
          (comp/button
            {:disabled (= 0 index)
             :onClick  #(reset! step-index (dec index))} "Back")
          (if (last-step? index)
            (composite/progress-button
              "Finish"
              (case @registry
                "dockerhub" #(dockerhub/add-user-handler)
                "v2" #(v2/create-registry-handler))
              processing?)
            (comp/button
              {:variant  "contained"
               :color    "primary"
               :disabled (not valid?)
               :onClick  #(reset! step-index (inc index))} "Next"))])))))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (reset! step-index 0)
      (reset! registry "dockerhub"))))

(rum/defc form < rum/reactive
                 mixin-init-form [route]
  (let [index (rum/react step-index)
        registry (rum/react registry)
        {:keys [public]} (state/react state/form-value-cursor)
        {:keys [valid? processing?]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/card
            {:className "Swarmpit-form-card"
             :style     {:maxWidth "400px"}
             :key       "dfc"}
            (comp/card-header
              {:className "Swarmpit-form-card-header"
               :key       "dfch"
               :title     "Add registry"})
            (comp/card-content
              {:key       "dfcc"
               :className "Swarmpit-table-card-content"}
              (comp/stepper
                {:activeStep  index
                 :key         "dfccs"
                 :orientation "vertical"}
                (step-item
                  0
                  true
                  registry-type-text
                  (registry-type-form registry))
                (step-item
                  1
                  valid?
                  (registry-text registry)
                  (registry-form registry route))
                (step-item
                  2
                  true
                  registry-publish-text
                  (registry-publish-form public)
                  processing?))))]]))))