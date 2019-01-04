(ns swarmpit.component.distribution.create
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.registry.create :as registry]
            [swarmpit.component.dockerhub.create :as dockerhub]))

(enable-console-print!)

(defonce step-index (atom 0))

(defonce distribution (atom "dockerhub"))

(def steps ["Choose type"
            "Provide details"
            "Share with team"])

(defn last-step?
  [index]
  (= index (- (count steps) 1)))

(def distribution-type-text
  "Specify distribution account type you would like to use to authenticate
   your private repositories.")

(defn- distribution-type-form [value]
  (comp/text-field
    {:fullWidth       true
     :key             "distrt"
     :label           "Distribution account type"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        (fn [e]
                        (let [type (-> e .-target .-value)]
                          (reset! distribution type)
                          (case type
                            "dockerhub" (dockerhub/reset-form)
                            "registry" (registry/reset-form))))}
    (comp/menu-item
      {:key   "dockerhub"
       :value "dockerhub"} "Dockerhub")
    (comp/menu-item
      {:key   "registry"
       :value "registry"} "Registry v2")))

(defn- distribution-text [distribution]
  (case distribution
    "dockerhub" "Please enter your docker login credentials. All your
                 linked namespaces access will be granted."
    "registry" "Please enter your custom v2 registry name & address.
                If you are using secured registry provide account
                credentials as well."))

(defn- distribution-form [distribution route]
  (case distribution
    "dockerhub" (dockerhub/form route)
    "registry" (registry/form route)))

(def distribution-publish-text
  "By default only user that has created distribution account can
   search & deploy private repositories. If you would like to share
   this access with other members of your team check the share button.")

(defn- distribution-publish-form [value]
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
  ([index text form]
   (step-item index text form false))
  ([index text form processing?]
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
              (case @distribution
                "dockerhub" #(dockerhub/add-user-handler)
                "registry" #(registry/create-registry-handler))
              processing?)
            (comp/button
              {:variant "contained"
               :color   "primary"
               :onClick #(reset! step-index (inc index))} "Next"))])))))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (reset! step-index 0)
      (reset! distribution "dockerhub"))))

(rum/defc form < rum/reactive
                 mixin-init-form [route]
  (let [index (rum/react step-index)
        distribution (rum/react distribution)
        {:keys [public]} (state/react state/form-value-cursor)
        {:keys [processing?]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:item true
             :xs   12
             :sm   6
             :md   4}
            (comp/card
              {:className "Swarmpit-form-card"
               :key       "dfc"}
              (comp/card-header
                {:className "Swarmpit-form-card-header"
                 :key       "dfch"
                 :title     "Add distribution"})
              (comp/card-content
                {:key       "dfcc"
                 :className "Swarmpit-table-card-content"}
                (comp/stepper
                  {:activeStep  index
                   :key         "dfccs"
                   :orientation "vertical"}
                  (step-item
                    0
                    distribution-type-text
                    (distribution-type-form distribution))
                  (step-item
                    1
                    (distribution-text distribution)
                    (distribution-form distribution route))
                  (step-item
                    2
                    distribution-publish-text
                    (distribution-publish-form public)
                    processing?)))))]]))))