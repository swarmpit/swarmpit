(ns swarmpit.component.service.create-config
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defonce step-index (atom 0))

(def steps ["General settings" "Ports" "Networks" "Mounts" "Secrets" "Environment variables" "Deployment"])

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

(def step-content-style
  {:minWidth "600px"})

(def stepper-style
  {:height "60px"})

(def form-previous-button-style
  {:margin "10px 10px 10px 20px"})

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
     :disabled   (= (- (count steps) 1) index)
     :onTouchTap (fn [] (step-next index))}))

(defn- step-item [index form]
  (comp/step
    {:key (str "step-" index)}
    (comp/step-button
      {:key                (str "step-btn-" index)
       :disableTouchRipple true
       :style              step-style
       :className          "step-label"
       :onClick            (fn [] (reset! step-index index))}
      (nth steps index))
    (comp/step-content
      {:key   (str "step-context-" index)
       :style step-content-style}
      form
      (form-previous-button index)
      (form-next-button index))))

(defn- create-service-handler
  []
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        networks (state/get-value networks/cursor)
        mounts (state/get-value mounts/cursor)
        secrets (state/get-value secrets/cursor)
        variables (state/get-value variables/cursor)
        deployment (state/get-value deployment/cursor)]
    (ajax/POST (routes/path-for-backend :service-create)
               {:format        :json
                :headers       {"Authorization" (storage/get "token")}
                :params        (-> settings
                                   (assoc :ports ports)
                                   (assoc :networks networks)
                                   (assoc :mounts mounts)
                                   (assoc :secrets secrets)
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
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")
                                       message (str "Service creation failed. Reason: " error)]
                                   (progress/unmount!)
                                   (message/mount! message)))})))

(rum/defc form < rum/reactive [networks volumes secrets]
  (let [index (rum/react step-index)
        {:keys [isValid]} (state/react settings/cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services "New service")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-service-handler}))]]
     (comp/mui
       (comp/stepper
         {:activeStep  index
          :linear      false
          :style       stepper-style
          :orientation "vertical"}
         (step-item 0 (settings/form false))
         (step-item 1 (ports/form-create))
         (step-item 2 (networks/form-create networks))
         (step-item 3 (mounts/form-create volumes))
         (step-item 4 (secrets/form-create secrets))
         (step-item 5 (variables/form-create))
         (step-item 6 (deployment/form))))]))

(defn- init-state
  [registry repository]
  (reset! step-index 0)
  (settings/image-tags-handler registry repository)
  (state/set-value {:repository  {:registry  registry
                                  :imageName repository
                                  :imageTag  ""
                                  :tags      []}
                    :serviceName ""
                    :mode        "replicated"
                    :replicas    1
                    :isValid     false} settings/cursor)
  (state/set-value [] ports/cursor)
  (state/set-value [] networks/cursor)
  (state/set-value [] mounts/cursor)
  (state/set-value [] secrets/cursor)
  (state/set-value [] variables/cursor)
  (state/set-value {:autoredeploy  false
                    :parallelism   1
                    :delay         0
                    :failureAction "pause"} deployment/cursor))

(defn mount!
  [registry repository networks volumes secrets]
  (init-state registry repository)
  (rum/mount (form networks volumes secrets) (.getElementById js/document "content")))