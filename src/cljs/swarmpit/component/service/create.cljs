(ns swarmpit.component.service.create
  (:require [swarmpit.material :as material]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-variables :as variables]
            [rum.core :as rum]))

(enable-console-print!)

(defonce step-index (atom 0))

(def steps ["General settings" "Ports" "Environment variables"])

(defmulti form-item identity)

(defmethod form-item 0 [_] (settings/form))

(defmethod form-item 1 [_] (ports/form))

(defmethod form-item 2 [_] (variables/form))

(defn- step-previous
  [index]
  (if (< 0 index)
    (reset! step-index (dec index))))

(defn- step-next
  [index]
  (if (> (count steps) index)
    (reset! step-index (inc index))))

(defn- step-items []
  (map-indexed
    (fn [index item]
      (material/step
        #js {:key index}
        (material/step-button
          #js {:onClick (fn [] (reset! step-index index))} item)))
    steps))

(rum/defc form < rum/reactive []
  (let [index (rum/react step-index)]
    [:div.form
     (material/theme
       (material/stepper
         #js {:activeStep index
              :linear     false
              :children   (clj->js (step-items))}))
     (form-item index)
     [:div.form-buttons
      (material/theme
        (material/flat-button
          #js {:label      "Previous"
               :disabled   (= 0 index)
               :onTouchTap (fn [] (step-previous index))
               :style      #js {:marginRight "12px"}}))
      (material/theme
        (material/raised-button
          #js {:label      "Next"
               :disabled   (= (- (count steps) 1) index)
               :onTouchTap (fn [] (step-next index))
               :primary    true}))]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "content")))