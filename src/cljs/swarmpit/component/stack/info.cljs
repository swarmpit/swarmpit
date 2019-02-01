(ns swarmpit.component.stack.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.chart :as chart]
            [material.component.list.basic :as list]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.action-menu :as menu]
            [swarmpit.component.service.list :as services]
            [swarmpit.component.network.list :as networks]
            [swarmpit.component.volume.list :as volumes]
            [swarmpit.component.config.list :as configs]
            [swarmpit.component.secret.list :as secrets]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [clojure.string :refer [includes?]]
            [clojure.contrib.inflect :as inflect]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.docker.utils :as utils]
            [material.component.label :as label]
            [swarmpit.router :as router]))

(enable-console-print!)

(defn- stack-services-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-services {:name stack-name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor)
                   (when (empty? response)
                     (router/not-found! {:error "stack is empty"})))}))

(defn- stack-networks-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-networks {:name stack-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:networks] response state/form-value-cursor))}))

(defn- stack-volumes-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-volumes {:name stack-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:volumes] response state/form-value-cursor))}))

(defn- stack-configs-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-configs {:name stack-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:configs] response state/form-value-cursor))}))

(defn- stack-secrets-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-secrets {:name stack-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:secrets] response state/form-value-cursor))}))

(defn- stackfile-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-file {:name stack-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:stackfile] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- delete-stack-handler
  [stack-name]
  (ajax/delete
    (routes/path-for-backend :stack-delete {:name stack-name})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :stack-list))
                   (message/info
                     (str "Stack " stack-name " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack removing failed. " (:error response))))}))

(defn- redeploy-stack-handler
  [stack-name]
  (message/info
    (str "Stack " stack-name " redeploy triggered."))
  (ajax/post
    (routes/path-for-backend :stack-redeploy {:name stack-name})
    {:on-success (fn [_]
                   (message/info
                     (str "Stack " stack-name " redeploy finished.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack redeploy failed. " (:error response))))}))

(defn- rollback-stack-handler
  [stack-name]
  (message/info
    (str "Stack " stack-name " rollback triggered."))
  (ajax/post
    (routes/path-for-backend :stack-rollback {:name stack-name})
    {:on-success (fn [_]
                   (message/info
                     (str "Stack " stack-name " rollback finished.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack rollback failed. " (:error response))))}))

(defn form-actions
  [stack-name stackfile]
  [{:onClick #(dispatch! (routes/path-for-frontend :stack-compose {:name stack-name}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit stack"}
   {:onClick  #(redeploy-stack-handler stack-name)
    :disabled (not (some? (:spec stackfile)))
    :more     true
    :icon     (comp/svg icon/redeploy-path)
    :name     "Redeploy stack"}
   {:onClick  #(rollback-stack-handler stack-name)
    :disabled (not (some? (:previousSpec stackfile)))
    :more     true
    :icon     (comp/svg icon/rollback-path)
    :name     "Rollback stack"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :name    "Delete stack"}])

(rum/defc form-services-graph < rum/static [services]
  (let [data (->> services
                  (map (fn [service]
                         (if (= "running" (:state service))
                           {:name  (:serviceName service)
                            :value 1
                            :color "#43a047"
                            :state (:state service)}
                           {:name  (:serviceName service)
                            :value 1
                            :color "#6c757d"
                            :state (:state service)})))
                  (into []))]
    (chart/pie
      data
      (str (count services) " " (inflect/pluralize-noun (count services) "service"))
      "Swarmpit-service-replicas-graph"
      "sservices-pie"
      {:formatter (fn [value name props]
                    (.-state (.-payload props)))})))

(defn- resource-chip
  [name count]
  (when (< 0 count)
    (comp/grid
      {:item true}
      (comp/chip {:key    name
                  :avatar (comp/avatar {} count)
                  :label  (inflect/pluralize-noun count name)}))))

(defn- add-external-status
  [render-metadata stack-name]
  (list/add-status
    render-metadata
    #(when (not (utils/in-stack? stack-name %))
       (html [:span.Swarmpit-table-status (label/info "external")]))))

(rum/defc form-general < rum/static [stack-name stackfile {:keys [services networks volumes configs secrets]}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     stack-name
       :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
       :action    (menu/menu
                    (form-actions stack-name stackfile)
                    :stackGeneralMenuAnchor
                    :stackGeneralMenuOpened)})
    (comp/grid
      {:container true
       :spacing   16}
      (comp/grid
        {:item true
         :xs   6}
        (comp/card-content
          {}
          (form-services-graph services)))
      (comp/grid
        {:item  true
         :xs    6
         :style {:display "flex"}}
        (comp/card-content
          {:style {:display "flex"}}
          (comp/grid
            {:container  true
             :spacing    8
             :direction  "column"
             :justify    "center"
             :alignItems "flex-start"}
            (resource-chip "network" (count networks))
            (resource-chip "volume" (count volumes))
            (resource-chip "config" (count configs))
            (resource-chip "secret" (count secrets))))))
    (comp/divider
      {})
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (form/item-id stack-name))))

(rum/defc form-services < rum/static [stack-name services]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Services")})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        (list/override-title
          services/render-metadata
          #(utils/trim-stack stack-name (:serviceName %))
          #(get-in % [:repository :image]))
        (sort-by :serviceName services)
        services/onclick-handler))))

(rum/defc form-networks < rum/static [stack-name networks]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Networks")})
    (if (empty? networks)
      (comp/card-content
        {}
        (html [:div "No networks in stack."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive
          (-> networks/render-metadata
              (list/override-title #(utils/alias :networkName stack-name %))
              (add-external-status stack-name))
          networks
          networks/onclick-handler)))))

(rum/defc form-volumes < rum/static [stack-name volumes]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Volumes")})
    (if (empty? volumes)
      (comp/card-content
        {}
        (html [:div "No volumes in stack."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive
          (-> volumes/render-metadata
              (list/override-title #(utils/alias :volumeName stack-name %))
              (add-external-status stack-name))
          volumes
          volumes/onclick-handler)))))

(rum/defc form-configs < rum/static [stack-name configs]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Configs")})
    (if (empty? configs)
      (comp/card-content
        {}
        (html [:div "No configs in stack."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/list
          (:list configs/render-metadata)
          (sort-by :configName configs)
          configs/onclick-handler)))))

(rum/defc form-secrets < rum/static [stack-name secrets]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Secrets")})
    (if (empty? secrets)
      (comp/card-content
        {}
        (html [:div "No secrets in stack."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/list
          (:list secrets/render-metadata)
          (sort-by :secretName secrets)
          secrets/onclick-handler)))))

(defn- init-form-state
  []
  (state/set-value {:menu?    false
                    :loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-state)
      (stack-services-handler name)
      (stack-networks-handler name)
      (stack-volumes-handler name)
      (stack-configs-handler name)
      (stack-secrets-handler name)
      (stackfile-handler name))))

(defn form-general-grid [stack-name stackfile item]
  (comp/grid
    {:item true
     :xs   12}
    (form-general stack-name stackfile item)))

(defn form-services-grid [stack-name services]
  (comp/grid
    {:item true
     :xs   12}
    (form-services stack-name services)))

(defn form-networks-grid [stack-name networks]
  (comp/grid
    {:item true
     :xs   12}
    (form-networks stack-name networks)))

(defn form-secrets-grid [stack-name secrets]
  (comp/grid
    {:item true
     :xs   12}
    (form-secrets stack-name secrets)))

(defn form-configs-grid [stack-name configs]
  (comp/grid
    {:item true
     :xs   12}
    (form-configs stack-name configs)))

(defn form-volumes-grid [stack-name volumes]
  (comp/grid
    {:item true
     :xs   12}
    (form-volumes stack-name volumes)))


(rum/defc form-info < rum/static [stack-name
                                  {:keys [services networks volumes configs secrets stackfile] :as item}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-stack-handler stack-name)
         "Delete stack?"
         "Delete")
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :sm   6
               :md   4}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-general-grid stack-name stackfile item)
                (form-secrets-grid stack-name secrets)
                (form-configs-grid stack-name configs)))
            (comp/grid
              {:item true
               :sm   6
               :md   8}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-services-grid stack-name services)
                (form-networks-grid stack-name networks)
                (form-volumes-grid stack-name volumes)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid stack-name stackfile item)
            (form-services-grid stack-name services)
            (form-networks-grid stack-name networks)
            (form-volumes-grid stack-name volumes)
            (form-secrets-grid stack-name secrets)
            (form-configs-grid stack-name configs)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [name]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info name item))))
