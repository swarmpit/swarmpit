(ns swarmpit.component.service.create-config
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
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
            [swarmpit.utils :refer [clean-nils]]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def doc-services-link "https://docs.docker.com/engine/swarm/services/")

(defn- create-service-handler
  []
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
      (routes/path-for-backend :services)
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
       :on-success (fn [{:keys [response origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :service-info (select-keys response [:id]))))
                     (message/info
                       (str "Service " (:id response) " has been created.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Service creation failed. " (:error response))))})))

(defn- init-form-state
  []
  (state/set-value {:processing? false
                    :active      0} state/form-state-cursor)
  (state/set-value {:valid? false
                    :tags   []} settings/form-state-cursor)
  (state/set-value {:volumes []} mounts/form-state-cursor)
  (state/set-value {:list []} secrets/form-state-cursor)
  (state/set-value {:list []} configs/form-state-cursor)
  (state/set-value {:list []} networks/form-state-cursor)
  (state/set-value {:list []} placement/form-state-cursor)
  (state/set-value {:names []} labels/form-state-cursor)
  (state/set-value {:valid? true} resources/form-state-cursor))

(defn- init-form-value
  [repository]
  (state/set-value {:repository  {:name repository
                                  :tag  ""}
                    :serviceName ""
                    :mode        "replicated"
                    :replicas    1} settings/form-value-cursor)
  (state/set-value [] ports/form-value-cursor)
  (state/set-value [] networks/form-value-cursor)
  (state/set-value [] mounts/form-value-cursor)
  (state/set-value [] secrets/form-value-cursor)
  (state/set-value [] configs/form-value-cursor)
  (state/set-value [] hosts/form-value-cursor)
  (state/set-value [] variables/form-value-cursor)
  (state/set-value [] labels/form-value-cursor)
  (state/set-value {:name "json-file"
                    :opts []} logdriver/form-value-cursor)
  (state/set-value {:autoredeploy  false
                    :restartPolicy {:condition "any"
                                    :delay     5
                                    :attempts  0}
                    :update        {:parallelism   1
                                    :delay         0
                                    :order         "stop-first"
                                    :failureAction "pause"}
                    :rollback      {:parallelism   1
                                    :delay         0
                                    :order         "stop-first"
                                    :failureAction "pause"}} deployment/form-value-cursor)
  (state/set-value {:reservation {:cpu    0.000
                                  :memory 0}
                    :limit       {:cpu    0.000
                                  :memory 0}} resources/form-value-cursor)
  (state/set-value [] placement/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [repository]} :params}]
      (init-form-state)
      (init-form-value repository)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (logdriver/drivers-handler)
      (secrets/secrets-handler)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (configs/configs-handler))
      (placement/placement-handler)
      (labels/labels-handler)
      (settings/tags-handler repository))))

(rum/defc form-general < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (settings/form false)))

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

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [active processing?]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-toolbar
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
               :aria-label     "scrollable auto tabs"}
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
                  "Create service"))
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
                  create-service-handler
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