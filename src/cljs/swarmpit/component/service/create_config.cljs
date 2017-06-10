(ns swarmpit.component.service.create-config
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
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

(def step-style
  {:backgroundColor "transparent"})

(def stepper-style
  {:height "60px"})

(def form-previous-button-style
  {:margin "10px"})

(def form-next-button-style
  {:marginBottom "10px"})

(defn- form-previous-button [index]
  (comp/raised-button
    {:label      "Previous"
     :key        "fpb"
     :style      form-previous-button-style
     :disabled   (= 0 index)
     :onTouchTap (fn [] (step-previous index))}))

(defn- form-next-button [index]
  (comp/raised-button
    {:label      "Next"
     :key        "fnb"
     :style      form-next-button-style
     :disabled   (= (- (count steps) 1) index)
     :onTouchTap (fn [] (step-next index))}))

(defn- step-items []
  (map-indexed
    (fn [index item]
      (comp/step
        {:key (str "step-" index)}
        (comp/step-button
          {:key                (str "step-btn-" index)
           :disableTouchRipple true
           :style              step-style
           :onClick            (fn [] (reset! step-index index))}
          item)
        (comp/step-content
          {:key (str "step-context-" index)}
          (form-item index)
          (form-previous-button index)
          (form-next-button index))))
    steps))

(defn- create-service-handler
  []
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        volumes (state/get-value volumes/cursor)
        variables (state/get-value variables/cursor)
        deployment (state/get-value deployment/cursor)]
    (ajax/POST (routes/path-for-backend :service-create)
               {:format        :json
                :headers       {"Authorization" (storage/get "token")}
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
                                   (dispatch!
                                     (routes/path-for-frontend :service-info {:id id}))
                                   (message/mount! message)))
                :error-handler (fn [{:keys [status response]}]
                                 (let [error (get response "error")
                                       message (str "Service creation failed. Status: " status " Reason: " error)]
                                   (progress/unmount!)
                                   (message/mount! message)))})))

(rum/defc form < rum/reactive []
  (let [index (rum/react step-index)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services "New service")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :primary    true
            :onTouchTap create-service-handler}))]]
     (comp/mui
       (comp/stepper
         {:activeStep  index
          :linear      false
          :style       stepper-style
          :orientation "vertical"}
         (step-items)))]))

(defn- init-state
  [registry repository]
  (settings/image-tags-handler registry repository)
  (state/set-value {:repository  {:registry  registry
                                  :imageName repository
                                  :imageTag  ""
                                  :tags      []}
                    :serviceName ""
                    :mode        "replicated"
                    :replicas    1} settings/cursor)
  (state/set-value [] ports/cursor)
  (state/set-value [] volumes/cursor)
  (state/set-value [] variables/cursor)
  (state/set-value {:autoredeploy false} deployment/cursor))

(defn mount!
  [registry repository]
  (init-state registry repository)
  (rum/mount (form) (.getElementById js/document "content")))