(ns swarmpit.component.service.create-form
  (:require [swarmpit.material :as material :refer [svg]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce step-index (atom 0))
(defonce image-state (atom nil))
(defonce replicas-state (atom 1))
(defonce mode-state (atom "replicated"))

(rum/defc settings-form < rum/reactive []
  [:form
   [:fieldset
    [:legend "General settings"]
    (material/form-row
      "IMAGE"
      (material/select-field #js {:value    (rum/react image-state)
                                  :onChange (fn [e i v] (reset! image-state v))
                                  :style    #js {:display  "inherit"
                                                 :fontSize "14px"}}
                             (material/menu-item #js {:key         1
                                                      :value       1
                                                      :primaryText "image1:latest"})
                             (material/menu-item #js {:key         2
                                                      :value       2
                                                      :primaryText "image2:latest"})))
    (material/form-row
      "SERVICE NAME"
      (material/text-field #js {:hintText "The name of your service"}))
    (material/form-row
      "MODE"
      (material/radio-button-group #js {:name            "status"
                                        :defaultSelected "replicated"
                                        :onChange        (fn [e v] (reset! mode-state v))
                                        :style           #js {:display   "flex"
                                                              :marginTop "14px"}}
                                   (material/radio-button #js {:label "Replicated"
                                                               :value "replicated"
                                                               :style #js {:width "170px"}})
                                   (material/radio-button #js {:label "Global"
                                                               :value "global"})))
    (if (= "replicated" (rum/react mode-state))
      (material/form-row
        (str "SERVICE REPLICAS" "  " "(" @replicas-state ")")
        (material/slider #js {:min          1
                              :max          50
                              :step         1
                              :defaultValue 1
                              :value        (rum/react replicas-state)
                              :onChange     (fn [e v] (reset! replicas-state v))
                              :sliderStyle  #js {:marginTop "14px"}})))
    (material/form-row
      "AUTOREDEPLOY"
      (material/toogle #js {:style #js {:marginTop "14px"}}))
    ]])

(rum/defc port-form < rum/reactive []
  [:form
   [:fieldset
    [:legend "Ports"]
    [:div.form-inline
     (material/theme
       (material/text-field #js {:style    #js {:width "150px"}
                                 :hintText "Container port"}))
     (material/theme
       (material/select-field #js {:style #js {:display  "inherit"
                                               :width    "100px"
                                               :fontSize "14px"}}
                              (material/menu-item #js {:key         1
                                                       :value       "tcp"
                                                       :primaryText "TCP"})
                              (material/menu-item #js {:key         2
                                                       :value       "udp"
                                                       :primaryText "UDP"})))
     (material/theme
       (material/text-field #js {:style    #js {:width "150px"}
                                 :hintText "Host port"}))
     (material/theme
       (material/checkbox #js {:style #js {:marginTop "14px"}}))
     (material/theme
       (svg #js {:style #js {:marginTop "14px"}} material/trash-icon))]]])

(defn step
  [name index]
  (material/step #js {}
                 (material/step-button #js {:onClick (fn [] (reset! step-index index))} name)))

(rum/defc form < rum/reactive []
  (let [index (rum/react step-index)]
    [:div.form
     (material/theme
       (material/stepper #js {:activeStep index
                              :linear     false}
                         (step "General settings" 0)
                         (step "Container configuration" 1)
                         (step "Ports" 2)
                         (step "Environment variables" 3)
                         (step "Volumes" 4)))
     ;(settings-form)
     (port-form)
     [:div.form-buttons
      (material/theme
        (material/flat-button #js {:label      "Previous"
                                   :disabled   (= 0 index)
                                   :onTouchTap (fn [] (if (< 0 index)
                                                        (reset! step-index (dec index))))
                                   :style      #js {:marginRight "12px"}}))
      (material/theme
        (material/raised-button #js {:label      (if (= 4 index) "Create" "Next")
                                     :onTouchTap (fn [] (if (> 4 index)
                                                          (reset! step-index (inc index))))
                                     :primary    true}))]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "content")))
