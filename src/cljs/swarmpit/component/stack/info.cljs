(ns swarmpit.component.stack.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.component.network.list :as networks]
            [swarmpit.component.volume.list :as volumes]
            [swarmpit.component.config.list :as configs]
            [swarmpit.component.secret.list :as secrets]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- stack-services-handler
  [stack-name]
  (ajax/get
    (routes/path-for-backend :stack-services {:name stack-name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

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

(def action-menu-style
  {:position   "relative"
   :marginTop  "13px"
   :marginLeft "66px"})

(def action-menu-item-style
  {:padding "0px 10px 0px 52px"})

(defn- form-action-menu [stack-name stackfile opened?]
  (comp/mui
    (comp/icon-menu
      {:iconButtonElement (comp/icon-button nil nil)
       :open              opened?
       :style             action-menu-style
       :onRequestChange   (fn [state] (state/update-value [:menu?] state state/form-state-cursor))}
      (comp/menu-item
        {:key           "action-edit"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/edit)
         :onClick       (fn []
                          (dispatch!
                            (routes/path-for-frontend :stack-edit {:name stack-name})))
         :primaryText   "Edit"})
      (comp/menu-item
        {:key           "action-redeploy"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/redeploy)
         :onClick       #(redeploy-stack-handler stack-name)
         :disabled      (not (some? (:spec stackfile)))
         :primaryText   "Redeploy"})
      (comp/menu-item
        {:key           "action-rollback"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/rollback)
         :onClick       #(rollback-stack-handler stack-name)
         :disabled      (not (some? (:previousSpec stackfile)))
         :primaryText   "Rollback"})
      (comp/menu-item
        {:key           "action-delete"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/trash)
         :onClick       #(delete-stack-handler stack-name)
         :primaryText   "Delete"}))))

(rum/defc form-services < rum/static [services]
  [:div.form-layout-group
   (form/section "Services")
   (list/table (map :name services/headers)
               services
               services/render-item
               services/render-item-keys
               services/onclick-handler)])

(rum/defc form-networks < rum/static [networks]
  (when (not-empty networks)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Networks")
     (list/table (map :name networks/headers)
                 networks
                 networks/render-item
                 networks/render-item-keys
                 networks/onclick-handler)]))

(rum/defc form-volumes < rum/static [volumes]
  (when (not-empty volumes)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Volumes")
     (list/table (map :name volumes/headers)
                 volumes
                 volumes/render-item
                 volumes/render-item-keys
                 volumes/onclick-handler)]))

(rum/defc form-configs < rum/static [configs]
  (when (not-empty configs)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Configs")
     (list/table (map :name configs/headers)
                 configs
                 configs/render-item
                 configs/render-item-keys
                 configs/onclick-handler)]))

(rum/defc form-secrets < rum/static [secrets]
  (when (not-empty secrets)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Secrets")
     (list/table (map :name configs/headers)
                 secrets
                 secrets/render-item
                 secrets/render-item-keys
                 secrets/onclick-handler)]))

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

(rum/defc form-info < rum/static [stack-name
                                  {:keys [services networks volumes configs secrets stackfile]}
                                  {:keys [menu?]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/stacks stack-name)]
    [:div.form-panel-right
     [:div
      (comp/mui
        (comp/raised-button
          {:onClick       (fn [_] (state/update-value [:menu?] true state/form-state-cursor))
           :icon          (comp/button-icon icon/expand-18)
           :labelPosition "before"
           :label         "Actions"}))
      (form-action-menu stack-name stackfile menu?)]]]
   [:div.form-layout
    (form-services services)
    (form-networks networks)
    (form-volumes volumes)
    (form-configs configs)
    (form-secrets secrets)]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [name]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info name item state))))