(ns swarmpit.component.service.edit
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.composite :as composite]
            [material.component.form :as form]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-configs :as configs]
            [swarmpit.component.service.form-hosts :as hosts]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.message :as message]
            [swarmpit.utils :refer [clean-nils]]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def doc-services-link "https://docs.docker.com/engine/swarm/services/")

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

(def general-keys
  [:repository
   :version
   :serviceName
   :mode
   :replicas
   :stack
   :agent
   :command
   :tty
   :user
   :dir
   :healthcheck
   :links
   :containerLabels])

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:state [:loading?]
     :on-success
            (fn [{:keys [response]}]
              (settings/tags-handler (-> response :repository :name))
              (state/set-value (select-keys response general-keys) settings/form-value-cursor)
              (state/set-value (-> (:ports response) (state/assoc-keys)) ports/form-value-cursor)
              (state/set-value (-> (:mounts response) (state/assoc-keys)) mounts/form-value-cursor)
              (state/set-value (->> (:secrets response)
                                    (map #(select-keys % [:secretName :secretTarget]))
                                    (state/assoc-keys)) secrets/form-value-cursor)
              (state/set-value (->> (:configs response)
                                    (map #(select-keys % [:configName :configTarget]))
                                    (state/assoc-keys)) configs/form-value-cursor)
              (state/set-value (-> (:hosts response) (state/assoc-keys)) hosts/form-value-cursor)
              (state/set-value (-> (:variables response) (state/assoc-keys)) variables/form-value-cursor)
              (state/set-value (-> (:labels response) (state/assoc-keys)) labels/form-value-cursor)
              (state/set-value (:logdriver response) logdriver/form-value-cursor)
              (state/set-value (-> (get-in response [:logdriver :opts]) (state/assoc-keys)) logdriver/form-value-opts-cursor)
              (state/set-value (:resources response) resources/form-value-cursor)
              (state/set-value (:deployment response) deployment/form-value-cursor))}))

(defn- service-networks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-networks {:id service-id})
    {:on-success
     (fn [{:keys [response]}]
       (state/set-value (->> response
                             (map #(select-keys % [:networkName :serviceAliases]))
                             (into [])) networks/form-value-cursor))}))

(defn- update-service-handler
  [service-id]
  (let [settings (state/get-value settings/form-value-cursor)
        ports (state/get-value ports/form-value-cursor)
        networks (state/get-value networks/form-value-cursor)
        secrets (state/get-value secrets/form-value-cursor)
        configs (state/get-value configs/form-value-cursor)
        hosts (state/get-value hosts/form-value-cursor)
        variables (state/get-value variables/form-value-cursor)
        labels (state/get-value labels/form-value-cursor)
        logdriver (state/get-value logdriver/form-value-cursor)
        resources (state/get-value resources/form-value-cursor)
        deployment (state/get-value deployment/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :service {:id service-id})
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts (mounts/normalize))
                       (assoc :secrets secrets)
                       (assoc :configs configs)
                       (assoc :hosts hosts)
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :logdriver logdriver)
                       (assoc :resources resources)
                       (assoc :deployment deployment)
                       (clean-nils))
       :state      [:processing?]
       :on-success (fn [{:keys [origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :service-info {:id service-id})))
                     (message/info
                       (str "Service " service-id " has been updated.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Service update failed. " (:error response))))})))

(defn- init-form-state
  []
  (state/set-value {:processing? false
                    :loading?    true
                    :active      0} state/form-state-cursor)
  (state/set-value {:valid? true
                    :tags   []} settings/form-state-cursor)
  (state/set-value {:volumes []} mounts/form-state-cursor)
  (state/set-value {:list []} secrets/form-state-cursor)
  (state/set-value {:list []} configs/form-state-cursor)
  (state/set-value {:list []} networks/form-state-cursor)
  (state/set-value {:list []} placement/form-state-cursor)
  (state/set-value {:names []} labels/form-state-cursor)
  (state/set-value {:valid? true} resources/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (service-handler id)
      (service-networks-handler id)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (logdriver/drivers-handler)
      (secrets/secrets-handler)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (configs/configs-handler))
      (placement/placement-handler)
      (labels/labels-handler))))

(rum/defc form-general < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (settings/form true)))

(rum/defc form-network < rum/static []
  (comp/grid
    {:container true
     :spacing   2}
    (comp/grid
      {:item true
       :xs   12}
      (networks/form))
    (comp/grid
      {:item true
       :xs   12}
      (form/section
        "Ports"
        (comp/button
          {:color   "primary"
           :onClick ports/add-item}
          (comp/svg icon/add-small-path) "Add port"))
      (ports/form))
    (comp/grid
      {:item true
       :xs   12}
      (form/section
        "Extra hosts"
        (comp/button
          {:color   "primary"
           :onClick hosts/add-item}
          (comp/svg icon/add-small-path) "Add host mapping"))
      (hosts/form))))

(rum/defc form-environment < rum/static []
  (comp/grid
    {:container true
     :spacing   2}
    (comp/grid
      {:item true
       :xs   12}
      (form/section
        "Variables"
        (comp/button
          {:color   "primary"
           :onClick variables/add-item}
          (comp/svg icon/add-small-path) "Add variable"))
      (variables/form))
    (comp/grid
      {:item true
       :xs   12}
      (form/section
        "Mounts"
        (comp/button
          {:color   "primary"
           :onClick mounts/add-item}
          (comp/svg icon/add-small-path) "Add mount"))
      (mounts/form))
    (when (<= 1.30 (state/get-value [:docker :api]))
      (comp/grid
        {:item true
         :xs   12}
        (form/section
          "Configs"
          (comp/button
            {:color   "primary"
             :onClick configs/add-item}
            (comp/svg icon/add-small-path) "Add config"))
        (configs/form)))
    (comp/grid
      {:item true
       :xs   12}
      (form/section
        "Secrets"
        (comp/button
          {:color   "primary"
           :onClick secrets/add-item}
          (comp/svg icon/add-small-path) "Add secret"))
      (secrets/form))))

(rum/defc form-logs < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (logdriver/form)))

(rum/defc form-resources < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (resources/form)))

(rum/defc form-deployment < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (deployment/form)))

(rum/defc form-edit < rum/reactive [id
                                    {:keys [settings]}
                                    {:keys [processing? active]}]
  (let [resources-state (state/react resources/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/container
            {:maxWidth  "md"
             :className "Swarmpit-container"}
            (comp/tabs
              {:value          active
               :onChange       (fn [_ v] (state/update-value [:active] v state/form-state-cursor))
               :indicatorColor "primary"
               :textColor      "primary"
               :variant        "scrollable"
               :scrollButtons  "auto"
               :aria-label     "tabs"}
              (comp/tab {:label "General"})
              (comp/tab {:label "Network"})
              (comp/tab {:label "Environment"})
              (comp/tab {:label "Resources"})
              (comp/tab {:label "Deployment"})
              (comp/tab {:label "Logs"}))
            (comp/divider {})
            (comp/card
              {:className "Swarmpit-form-card Swarmpit-tabs Swarmpit-fcard"}
              (comp/box
                {:className "Swarmpit-fcard-header"}
                (comp/typography
                  {:className "Swarmpit-fcard-header-title"
                   :variant   "h6"
                   :component "div"}
                  "Edit service"))
              (comp/card-content
                {:className "Swarmpit-fcard-content"}
                (common/tab-panel
                  {:value active
                   :index 0}
                  (form-general))
                (common/tab-panel
                  {:value active
                   :index 1}
                  (form-network))
                (common/tab-panel
                  {:value active
                   :index 2}
                  (form-environment))
                (common/tab-panel
                  {:value active
                   :index 3}
                  (form-resources))
                (common/tab-panel
                  {:value active
                   :index 4}
                  (form-deployment))
                (common/tab-panel
                  {:value active
                   :index 5}
                  (form-logs)))
              (comp/card-actions
                {:className "Swarmpit-fcard-actions"}
                (composite/progress-button
                  "Deploy"
                  #(update-service-handler id)
                  processing?
                  false
                  {:startIcon (comp/svg {} icon/rocket-path)})
                (html [:div.grow])
                (comp/button
                  {:variant  "text"
                   :disabled (= 0 active)
                   :onClick  #(state/update-value [:active] (- active 1) state/form-state-cursor)}
                  "Previous")
                (comp/button
                  {:variant  "text"
                   :disabled (= 5 active)
                   :onClick  #(state/update-value [:active] (+ active 1) state/form-state-cursor)}
                  "Next"))))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/scroll-to-section [{{:keys [id]} :params}]
  (let [state (state/react state/form-state-cursor)
        service (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit id service state))))