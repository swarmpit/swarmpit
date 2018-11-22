(ns swarmpit.component.service.create-config
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-configs :as configs]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- create-service-handler
  []
  (let [settings (state/get-value settings/form-value-cursor)
        ports (state/get-value ports/form-value-cursor)
        networks (state/get-value networks/form-value-cursor)
        secrets (state/get-value secrets/form-value-cursor)
        configs (state/get-value configs/form-value-cursor)
        variables (state/get-value variables/form-value-cursor)
        labels (state/get-value labels/form-value-cursor)
        logdriver (state/get-value logdriver/form-value-cursor)
        resources (state/get-value resources/form-value-cursor)
        deployment (state/get-value deployment/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :service-create)
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts (mounts/normalize))
                       (assoc :secrets (when-not (empty? (state/get-value (conj secrets/form-state-cursor :list))) secrets))
                       (assoc :configs (when-not (empty? (state/get-value (conj configs/form-state-cursor :list))) configs))
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :logdriver logdriver)
                       (assoc :resources resources)
                       (assoc :deployment deployment))
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
  (state/set-value {:processing? false} state/form-state-cursor)
  (state/set-value {:valid?             false
                    :tagMenuSuggestions []
                    :tags               []} settings/form-state-cursor)
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
      ;(secrets/secrets-handler)
      ;(when (<= 1.30 (state/get-value [:docker :api]))
      ;  (configs/configs-handler))
      ;(placement/placement-handler)
      ;(labels/labels-handler)
      (settings/tags-handler repository))))

(rum/defc form-settings < rum/static []
  [:div.form-layout-group
   (form/subsection "General settings")
   (settings/form false)])

(rum/defc form-ports < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Ports" ports/add-item)
   (ports/form)])

(rum/defc form-networks < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Networks" networks/add-item)
   (networks/form)])

(rum/defc form-mounts < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Mounts" mounts/add-item)
   (mounts/form)])

(rum/defc form-secrets < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Secrets" secrets/add-item)
   (secrets/form)])

(rum/defc form-configs < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Configs" configs/add-item)
   (configs/form)])

(rum/defc form-variables < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Environment Variables" variables/add-item)
   (variables/form)])

(rum/defc form-labels < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Labels" labels/add-item)
   (labels/form)])

(rum/defc form-logdriver < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/subsection "Logging")
   (logdriver/form)])

(rum/defc form-resources < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/subsection "Resources")
   (resources/form)])

(rum/defc form-deployment < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/subsection "Deployment")
   (deployment/form)])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [settings-state (state/react settings/form-state-cursor)
        resources-state (state/react resources/form-state-cursor)
        {:keys [processing?]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/paper
            {:className "Swarmpit-paper Swarmpit-form-context"
             :elevation 0}

            (comp/grid
              {:container true
               :spacing   40}
              (comp/grid
                {:item true
                 :xs   12}
                (form/section
                  "General")
                (settings/form false))
              (comp/grid
                {:item true
                 :xs   12}
                (form/section
                  "Ports"
                  (comp/button
                    {:color   "primary"
                     :onClick ports/add-item}
                    (comp/svg icon/add-small) "Add port"))
                (ports/form))
              (comp/grid
                {:item true
                 :xs   12}
                (form/section
                  "Networks"
                  (comp/button
                    {:color   "primary"
                     :onClick networks/add-item}
                    (comp/svg icon/add-small) "Add network"))
                (networks/form))
              (comp/grid
                {:item true
                 :xs   12}
                (form/section
                  "Mounts"
                  (comp/button
                    {:color   "primary"
                     :onClick mounts/add-item}
                    (comp/svg icon/add-small) "Add mount"))
                (mounts/form))


              ;(comp/grid
              ;  {:item true
              ;   :xs   12
              ;   :sm   6}
              ;  (comp/typography
              ;    {:variant      "h6"
              ;     :gutterBottom true} "IPAM")
              ;  (section-ipam item))
              ;(comp/grid
              ;  {:item true
              ;   :xs   12}
              ;  (comp/typography
              ;    {:variant      "h6"
              ;     :gutterBottom true} "Driver")
              ;  (section-driver item plugins))
              )

            )


          ;(form-settings)

          ;(form-ports)
          ;(form-networks)
          ;(form-mounts)
          ;(form-secrets)
          ;(when (<= 1.30 (state/get-value [:docker :api]))
          ;  (form-configs))
          ;(form-variables)
          ;(form-labels)
          ;(form-logdriver)
          ;(form-resources)
          ;(form-deployment)

          ]]))))


;[:div
; [:div.form-panel
;  [:div.form-panel-left
;   (panel/info icon/services "New service")]
;  [:div.form-panel-right
;   (comp/progress-button
;     {:label      "Create"
;      :disabled   (or (not (:valid? settings-state))
;                      (not (:valid? resources-state)))
;      :primary    true
;      :onTouchTap create-service-handler} processing?)]]
; [:div.form-layout
;  (form-settings)
;  (form-ports)
;  (form-networks)
;  (form-mounts)
;  (form-secrets)
;  (when (<= 1.30 (state/get-value [:docker :api]))
;    (form-configs))
;  (form-variables)
;  (form-labels)
;  (form-logdriver)
;  (form-resources)
;  (form-deployment)]]