(ns swarmpit.component.service.edit
  (:require [swarmpit.material :as material]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-variables :as variables]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  (let [id (get item "ID")]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:href    "/#/services/create"
                :label   "Save"
                :primary true
                :style   #js {:marginRight "12px"}}))
       (material/theme
         (material/raised-button
           #js {:href  "/#/services/create"
                :label "Back"}))]]
     [:div.form-view
      [:div.form-view-group
       (material/form-view-section "General settings")
       (settings/form true)]
      [:div.form-view-group
       (material/form-view-section "Ports")
       (ports/form)]
      [:div.form-view-group
       (material/form-view-section "Environment variables")
       (variables/form)]]]))

(defn- init-settings-state
  [item]
  (let [service-image (get-in item ["Spec" "TaskTemplate" "ContainerSpec" "Image"])
        service-name (get-in item ["Spec" "Name"])
        service-mode (first (keys (get-in item ["Spec" "Mode"])))]
    (reset! settings/state {:image        service-image
                            :serviceName  service-name
                            :mode         service-mode
                            :replicas     1
                            :autoredeploy false})))

(defn- init-ports-state
  [item]
  (let [service-ports (get-in item ["Spec" "EndpointSpec" "Ports"])]
    (reset! ports/state (->> service-ports
                             (map (fn [item]
                                    {:containerPort (get item "TargetPort")
                                     :protocol      (get item "Protocol")
                                     :published     false
                                     :hostPort      (get item "PublishedPort")}))
                             (into [])))))

(defn- init-variables-state
  []
  (reset! variables/state []))

(defn- init-state
  [item]
  (init-settings-state item)
  (init-ports-state item)
  (init-variables-state))

(defn mount!
  [item]
  (init-state item)
  (rum/mount (form item) (.getElementById js/document "content")))
