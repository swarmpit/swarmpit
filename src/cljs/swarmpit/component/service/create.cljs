(ns swarmpit.component.service.create
  (:require [swarmpit.uri :refer [dispatch!]]
            [swarmpit.material :as material]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defonce step-index (atom 0))

(def steps ["General settings" "Ports" "Volumes" "Environment variables" "Deployment"])

(defmulti form-item identity)

(defmethod form-item 0 [_] (settings/form false))

(defmethod form-item 1 [_] (ports/form))

(defmethod form-item 2 [_] (volumes/form))

(defmethod form-item 3 [_] (variables/form))

(defmethod form-item 4 [_] (deployment/form))

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
          #js {:disableTouchRipple true
               :style              #js {:backgroundColor "transparent"}
               :onClick            (fn [] (reset! step-index index))}
          item)))
    steps))

(defn- create-service-handler
  []
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        volumes (state/get-value volumes/cursor)
        variables (state/get-value variables/cursor)
        deployment (state/get-value deployment/cursor)]
    (ajax/POST "/services"
               {:format        :json
                :params        (-> settings
                                   (assoc :ports ports)
                                   (assoc :volumes volumes)
                                   (assoc :variables variables)
                                   (assoc :deployment deployment))
                :finally       (progress/mount!)
                :handler       (fn [response]
                                 (let [id (get response "ID")
                                       message (str "Service " id " has been created.")]
                                   (progress/unmount!)
                                   (dispatch! (str "/#/services/" id))
                                   (message/mount! message)))
                :error-handler (fn [{:keys [status status-text]}]
                                 (let [message (str "Service creation failed. Status: " status " Reason: " status-text)]
                                   (progress/unmount!)
                                   (message/mount! message)))})))

(rum/defc form < rum/reactive []
  (let [index (rum/react step-index)]
    [:div
     (material/theme
       (material/stepper
         #js {:activeStep index
              :linear     false
              :style      #js {:background "rgb(245, 245, 245)"
                               :height     "60px"}
              :children   (clj->js (step-items))}))
     (form-item index)
     [:div.form-panel.form-buttons
      [:div.form-panel-left
       (material/theme
         (material/raised-button
           #js {:label      "Previous"
                :disabled   (= 0 index)
                :onTouchTap (fn [] (step-previous index))
                :style      #js {:marginRight "12px"}}))
       (material/theme
         (material/raised-button
           #js {:label      "Next"
                :disabled   (= (- (count steps) 1) index)
                :onTouchTap (fn [] (step-next index))}))]
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:label      "Create"
                :primary    true
                :onTouchTap create-service-handler}))]]]))

(defn- init-state
  []
  (state/set-value {:image       nil
                    :serviceName ""
                    :mode        "replicated"
                    :replicas    1} settings/cursor)
  (state/set-value [] ports/cursor)
  (state/set-value [] volumes/cursor)
  (state/set-value [] variables/cursor)
  (state/set-value {:autoredeploy false} deployment/cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))