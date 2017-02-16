(ns swarmpit.component.service.create-form
  (:require [swarmpit.material :as material :refer [svg]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce step-index (atom 0))
(defonce image-state (atom nil))
(defonce replicas-state (atom 1))
(defonce mode-state (atom "replicated"))

(defonce state (atom {:ports []}))

(defn- add-port
  "Create new port configuration"
  []
  (swap! state update-in [:ports]
         (fn [p] (conj p {:containerPort ""
                          :protocol      "tcp"
                          :published     false
                          :hostPort      ""}))))

(defn- remove-port
  "Remove port configuration"
  [index]
  (swap! state update-in [:ports]
         (fn [p] (vec (concat
                        (subvec p 0 index)
                        (subvec p (inc index)))))))

(defn- update-port
  "Update port configuration"
  [index k v]
  (swap! state update-in [:ports]
         (fn [p] (assoc-in p [index k] v))))

(rum/defc settings-form < rum/reactive []
  [:form
   [:fieldset
    [:legend "General settings"]
    (material/form-row
      "IMAGE"
      (material/select-field
        #js {:value    (rum/react image-state)
             :onChange (fn [e i v] (reset! image-state v))
             :style    #js {:display  "inherit"
                            :fontSize "14px"}}
        (material/menu-item
          #js {:key         1
               :value       1
               :primaryText "image1:latest"})
        (material/menu-item
          #js {:key         2
               :value       2
               :primaryText "image2:latest"})))
    (material/form-row
      "SERVICE NAME"
      (material/text-field
        #js {:hintText "The name of your service"}))
    (material/form-row
      "MODE"
      (material/radio-button-group
        #js {:name            "status"
             :defaultSelected "replicated"
             :onChange        (fn [e v] (reset! mode-state v))
             :style           #js {:display   "flex"
                                   :marginTop "14px"}}
        (material/radio-button
          #js {:label "Replicated"
               :value "replicated"
               :style #js {:width "170px"}})
        (material/radio-button
          #js {:label "Global"
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
      (material/toogle
        #js {:style #js {:marginTop "14px"}}))]])

(defn port-form-row [port index]
  (material/table-row
    #js {:key           index
         :rowNumber     index
         :displayBorder false}
    (material/table-row-column
      nil
      (material/text-field
        #js {:id       "containerPort"
             :value    (:containerPort port)
             :onChange (fn [e v] (update-port index :containerPort v))}))
    (material/table-row-column
      nil
      (material/select-field
        #js {:value      (:protocol port)
             :onChange   (fn [e i v] (update-port index :protocol v))
             :style      #js {:display "inherit"}
             :labelStyle #js {:lineHeight "45px"
                              :top        2}}
        (material/menu-item
          #js {:key         1
               :value       "tcp"
               :primaryText "TCP"})
        (material/menu-item
          #js {:key         2
               :value       "udp"
               :primaryText "UDP"})))
    (material/table-row-column
      nil
      (material/checkbox
        #js {:checked (:published port)
             :onCheck (fn [e v] (update-port index :published v))}))
    (material/table-row-column
      nil
      (material/text-field
        #js {:id       "hostPort"
             :value    (:hostPort port)
             :onChange (fn [e v] (update-port index :hostPort v))}))
    (material/table-row-column
      nil
      (material/icon-button
        #js {:onClick (fn [] (remove-port index))}
        (material/svg
          #js {:hoverColor "red"}
          material/trash-icon)))))

(defn port-form [ports]
  [:form
   [:fieldset
    [:legend "Ports"]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header
          #js {:displaySelectAll  false
               :adjustForCheckbox false
               :style             #js {:border "none"}}
          (material/table-row
            #js {:displayBorder false}
            (material/table-header-column nil "Container port")
            (material/table-header-column nil "Protocol")
            (material/table-header-column nil "Published")
            (material/table-header-column nil "Host port")
            (material/table-header-column
              nil
              (material/icon-button
                #js {:onClick add-port}
                (material/svg
                  #js {:hoverColor "#437f9d"}
                  material/plus-icon)))))
        (material/table-body
          #js {:displayRowCheckbox false}
          (for [index (range (count ports))]
            (port-form-row (nth ports index) index)))))]])

(defn step
  [name index]
  (material/step
    nil
    (material/step-button #js {:onClick (fn [] (reset! step-index index))} name)))

(rum/defc form < rum/reactive []
  (let [{:keys [ports]} (rum/react state)
        index (rum/react step-index)]
    [:div.form
     (material/theme
       (material/stepper
         #js {:activeStep index
              :linear     false}
         (step "General settings" 0)
         (step "Container configuration" 1)
         (step "Ports" 2)
         (step "Environment variables" 3)
         (step "Volumes" 4)))
     ;(settings-form)
     (port-form ports)
     [:div.form-buttons
      (material/theme
        (material/flat-button
          #js {:label      "Previous"
               :disabled   (= 0 index)
               :onTouchTap (fn [] (if (< 0 index)
                                    (reset! step-index (dec index))))
               :style      #js {:marginRight "12px"}}))
      (material/theme
        (material/raised-button
          #js {:label      (if (= 4 index) "Create" "Next")
               :onTouchTap (fn [] (if (> 4 index)
                                    (reset! step-index (inc index))))
               :primary    true}))]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "content")))