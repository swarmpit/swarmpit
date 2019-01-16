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
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def doc-services-link "https://docs.docker.com/engine/swarm/services/")

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:state [:loading?]
     :on-success
            (fn [{:keys [response]}]
              (settings/tags-handler (-> response :repository :name))
              (state/set-value (select-keys response [:repository :version :serviceName :mode :replicas :stack :command]) settings/form-value-cursor)
              (state/set-value (:ports response) ports/form-value-cursor)
              (state/set-value (:mounts response) mounts/form-value-cursor)
              (state/set-value (->> (:secrets response)
                                    (map #(select-keys % [:secretName :secretTarget]))
                                    (into [])) secrets/form-value-cursor)
              (state/set-value (->> (:configs response)
                                    (map #(select-keys % [:configName :configTarget]))
                                    (into [])) configs/form-value-cursor)
              (state/set-value (:variables response) variables/form-value-cursor)
              (state/set-value (:labels response) labels/form-value-cursor)
              (state/set-value (:logdriver response) logdriver/form-value-cursor)
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
        variables (state/get-value variables/form-value-cursor)
        labels (state/get-value labels/form-value-cursor)
        logdriver (state/get-value logdriver/form-value-cursor)
        resources (state/get-value resources/form-value-cursor)
        deployment (state/get-value deployment/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :service-update {:id service-id})
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
                    :loading?    true} state/form-state-cursor)
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
      (secrets/secrets-handler)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (configs/configs-handler))
      (placement/placement-handler)
      (labels/labels-handler))))

(rum/defc form-settings < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgigs"
     :xs   12}
    (form/section
      "General")
    (rum/with-key
      (settings/form true) "sefgss")))

(rum/defc form-networks < rum/static []
  (comp/grid
    {:item    true
     :key     "secgifccgins"
     ;:style {:backgroundColor "#F5F5F6"
     ;        :borderTop       "1px solid rgba(0, 0, 0, 0.06)"
     ;        :borderBottom    "1px solid rgba(0, 0, 0, 0.06)"
     ;        :padding         "46px"}
     :gutters true
     :xs      12}
    (form/section
      "Networks")
    (rum/with-key
      (networks/form) "sefns")))

(rum/defc form-ports < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgip"
     :xs   12}
    (form/section
      "Ports"
      (comp/button
        {:color   "primary"
         :onClick ports/add-item}
        (comp/svg
          {:key "sefpsico"} icon/add-small-path) "Add port"))
    (rum/with-key
      (ports/form) "sefps")))

(rum/defc form-mounts < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgicm"
     :xs   12}
    (form/section
      "Mounts"
      (comp/button
        {:color   "primary"
         :onClick mounts/add-item}
        (comp/svg
          {:key "sefmsico"} icon/add-small-path) "Add mount"))
    (rum/with-key
      (mounts/form) "sefms")))

(rum/defc form-secrets < rum/reactive []
  (comp/grid
    {:item true
     :key  "secgifccgisec"
     :xs   12}
    (form/section
      "Secrets"
      (comp/button
        {:color   "primary"
         :onClick secrets/add-item}
        (comp/svg
          {:key "sefssico"} icon/add-small-path) "Add secret"))
    (rum/with-key
      (secrets/form) "sefss")))

(rum/defc form-configs < rum/reactive []
  (comp/grid
    {:item true
     :key  "secgifccgic"
     :xs   12}
    (form/section
      "Configs"
      (comp/button
        {:color   "primary"
         :onClick configs/add-item}
        (comp/svg
          {:key "sefcsico"} icon/add-small-path) "Add config"))
    (rum/with-key
      (configs/form) "sefcs")))

(rum/defc form-variables < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgiev"
     :xs   12}
    (form/section
      "Environment variables"
      (comp/button
        {:color   "primary"
         :onClick variables/add-item}
        (comp/svg
          {:key "sefevsico"} icon/add-small-path) "Add variable"))
    (rum/with-key
      (variables/form) "sefevs")))

(rum/defc form-labels < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgil"
     :xs   12}
    (form/section
      "Labels"
      (comp/button
        {:color   "primary"
         :onClick labels/add-item}
        (comp/svg
          {:key "seflasico"} icon/add-small-path) "Add label"))
    (rum/with-key
      (labels/form) "seflas")))

(rum/defc form-logdriver < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgild"
     :xs   12}
    (form/section
      "Logging")
    (rum/with-key
      (logdriver/form) "sefls")))

(rum/defc form-resources < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgir"
     :xs   12}
    (form/section
      "Resources")
    (rum/with-key
      (resources/form) "sefrs")))

(rum/defc form-deployment < rum/static []
  (comp/grid
    {:item true
     :key  "secgifccgid"
     :xs   12}
    (form/section
      "Deployment")
    (rum/with-key
      (deployment/form) "sefds")))

(rum/defc form-edit < rum/reactive [id
                                    {:keys [settings]}
                                    {:keys [processing?]}]
  (let [resources-state (state/react resources/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :key       "secg"
             :spacing   40}
            (comp/grid
              {:item true
               :key  "secgif"
               :xs   12
               :sm   12
               :md   12
               :lg   8
               :xl   8}
              (comp/card
                {:className "Swarmpit-form-card"
                 :key       "secgifc"}
                (comp/card-header
                  {:className "Swarmpit-form-card-header"
                   :key       "secgifch"
                   :title     (html [:span "Editing " [:span.Swarmpit-secondary-title (:serviceName settings)]])})
                (comp/card-content
                  {:key "secgifcc"
                   ;:style {:padding 0}
                   }
                  (comp/grid
                    {:container true
                     :key       "secgifcccg"
                     :spacing   40}
                    (rum/with-key
                      (form-settings) "secgifcccgfs")
                    (rum/with-key
                      (form-networks) "secgifcccgfnet")
                    (rum/with-key
                      (form-ports) "secgifcccgfp")
                    (rum/with-key
                      (form-mounts) "secgifcccgfm")
                    (rum/with-key
                      (form-secrets) "secgifcccgfse")
                    (when (<= 1.30 (state/get-value [:docker :api]))
                      (rum/with-key
                        (form-configs) "secgifcccgfc"))
                    (rum/with-key
                      (form-variables) "secgifcccgfv")
                    (rum/with-key
                      (form-labels) "secgifcccgfl")
                    (rum/with-key
                      (form-logdriver) "secgifcccgfld")
                    (rum/with-key
                      (form-resources) "secgifcccgfr")
                    (rum/with-key
                      (form-deployment) "secgifcccgfd"))
                  (html
                    [:div {:class "Swarmpit-form-buttons"
                           :key   "secgifccbtn"}
                     (composite/progress-button
                       "Save"
                       #(update-service-handler id)
                       processing?)]))))
            (comp/grid
              {:item true
               :key  "secgid"
               :xs   12
               :sm   12
               :md   12
               :lg   4
               :xl   4}
              (form/open-in-new "Learn more about compose" doc-services-link)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/scroll-to-section [{{:keys [id]} :params}]
  (let [state (state/react state/form-state-cursor)
        service (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit id service state))))