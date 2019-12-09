(ns swarmpit.component.registry.create
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.common :as common]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.registry-v2.create :as v2]
            [swarmpit.component.registry-ecr.create :as ecr]
            [swarmpit.component.registry-acr.create :as acr]
            [swarmpit.component.registry-gitlab.create :as gitlab]
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

(defn- registry-type-form-item [value icon title]
  (comp/menu-item
    {:value value}
    (comp/list-item-icon
      {}
      (comp/svg icon))
    (comp/list-item-text
      {:primary   title
       :className "Swarmpit-repo-registry-item"})))

(defn- registry-type-form [value]
  (comp/text-field
    {:fullWidth       true
     :label           "Registry account type"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-select-icon"}
     :onChange        (fn [e]
                        (let [type (-> e .-target .-value)]
                          (reset! registry type)
                          (case type
                            "dockerhub" (dockerhub/reset-form)
                            "v2" (v2/reset-form)
                            "ecr" (ecr/reset-form)
                            "acr" (acr/reset-form)
                            "gitlab" (gitlab/reset-form))))}
    (registry-type-form-item "dockerhub" icon/docker-path "Dockerhub")
    (registry-type-form-item "v2" icon/registries-path "Registry v2")
    (registry-type-form-item "ecr" icon/amazon-path "Amazon ECR")
    (registry-type-form-item "acr" icon/azure-path "Azure ACR")
    (registry-type-form-item "gitlab" icon/gitlab-path "Gitlab registry")))

(defn- registry-text [registry]
  (case registry
    "dockerhub" dockerhub/text
    "v2" v2/text
    "ecr" ecr/text
    "acr" acr/text
    "gitlab" gitlab/text))

(defn- registry-form [registry route]
  (case registry
    "dockerhub" (dockerhub/form route)
    "v2" (v2/form route)
    "ecr" (ecr/form route)
    "acr" (acr/form route)
    "gitlab" (gitlab/form route)))

(def registry-publish-text
  "Specify registry access level across Swarmpit platform")

(defn- registry-publish-form [value]
  (comp/form-control
    {:component "fieldset"
     :key       "role-f"
     :margin    "normal"}
    (comp/form-label
      {:key "rolel"} "Make account Public")
    (comp/form-helper-text
      {} "Means that anyone can search & deploy private repositories from this account")
    (comp/form-control-label
      {:control (comp/switch
                  {:checked  value
                   :value    (str value)
                   :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)})})))

(defn buttons
  [index valid? processing?]
  (comp/box
    {:className "Swarmpit-form-buttons"}
    (comp/button
      {:disabled (= 0 index)
       :onClick  #(reset! step-index (dec index))} "Back")
    (if (last-step? index)
      (composite/progress-button
        "Finish"
        (case @registry
          "dockerhub" #(dockerhub/add-user-handler)
          "v2" #(v2/create-registry-handler)
          "ecr" #(ecr/create-registry-handler)
          "acr" #(acr/create-registry-handler)
          "gitlab" #(gitlab/create-registry-handler))
        processing?)
      (comp/button
        {:variant  "contained"
         :color    "primary"
         :disabled (not valid?)
         :onClick  #(reset! step-index (inc index))} "Next"))))

(defn vertical-step-item
  ([index valid? text form]
   (vertical-step-item index valid? text form false))
  ([index valid? text form processing?]
   (comp/step
     {}
     (comp/step-label
       {}
       (nth steps index))
     (comp/step-content
       {}
       (comp/typography
         {:variant   "body2"
          :className "Swarmpit-fcard-message"} text)
       (comp/box {} form)
       (buttons index valid? processing?)))))

(defn horizontal-step-item
  ([index valid? text form]
   (horizontal-step-item index valid? text form false))
  ([index valid? text form processing?]
   (html
     [:div
      (comp/typography
        {:variant   "body2"
         :className "Swarmpit-fcard-message"} text)
      (comp/box {} form)
      (buttons index valid? processing?)])))

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
          (comp/hidden
            {:xsDown         true
             :implementation "js"}
            (comp/container
              {:maxWidth "md"}
              (comp/stepper
                {:className  "Swarmpit-stepper Swarmpit-stepper-horizontal"
                 :activeStep index}
                (comp/step
                  {}
                  (comp/step-label
                    {}
                    (nth steps 0)))
                (comp/step
                  {}
                  (comp/step-label
                    {}
                    (nth steps 1)))
                (comp/step
                  {}
                  (comp/step-label
                    {}
                    (nth steps 2))))
              (comp/card
                {:className "Swarmpit-form-card Swarmpit-fcard"}
                (comp/box
                  {:className "Swarmpit-fcard-header"}
                  (comp/typography
                    {:className "Swarmpit-fcard-header-title"
                     :variant   "h6"
                     :component "div"}
                    "Link registry"))
                (comp/card-content
                  {:className "Swarmpit-fcard-content"}
                  (case index
                    0 (horizontal-step-item
                        index
                        true
                        registry-type-text
                        (registry-type-form registry))
                    1 (horizontal-step-item
                        index
                        valid?
                        (registry-text registry)
                        (registry-form registry route))
                    2 (horizontal-step-item
                        index
                        true
                        registry-publish-text
                        (registry-publish-form public)
                        processing?))))))
          (comp/hidden
            {:smUp           true
             :implementation "js"}
            (comp/card
              {:className "Swarmpit-form-card Swarmpit-fcard"}
              (comp/box
                {:className "Swarmpit-fcard-header"}
                (comp/typography
                  {:className "Swarmpit-fcard-header-title"
                   :variant   "h6"
                   :component "div"}
                  "Link registry"))
              (comp/card-content
                {:className "Swarmpit-fcard-content"}
                (comp/stepper
                  {:className   "Swarmpit-stepper Swarmpit-stepper-vertical"
                   :activeStep  index
                   :orientation "vertical"}
                  (vertical-step-item
                    0
                    true
                    registry-type-text
                    (registry-type-form registry))
                  (vertical-step-item
                    1
                    valid?
                    (registry-text registry)
                    (registry-form registry route))
                  (vertical-step-item
                    2
                    true
                    registry-publish-text
                    (registry-publish-form public)
                    processing?)))))]]))))