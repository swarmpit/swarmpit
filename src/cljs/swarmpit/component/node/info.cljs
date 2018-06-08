(ns swarmpit.component.node.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.contrib.humanize :as humanize]
            [rum.core :as rum]))

(enable-console-print!)

(defn resources
  [node]
  (let [cpu (-> node :resources :cpu (int))
        memory-bytes (-> node :resources :memory (* 1024 1024))
        disk-bytes (-> node :stats :disk :total)]
    (let [core-stats (str cpu " " (clojure.contrib.inflect/pluralize-noun cpu "core") ", "
                          (humanize/filesize memory-bytes :binary false) " memory")]
      (if (some? disk-bytes)
        (str core-stats ", " (humanize/filesize disk-bytes :binary false) " disk")
        core-stats))))

(defn- node-tasks-handler
  [node-id]
  (ajax/get
    (routes/path-for-backend :node-tasks {:id node-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:tasks] response state/form-value-cursor))}))

(defn- node-handler
  [node-id]
  (ajax/get
    (routes/path-for-backend :node {:id node-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:node] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (node-handler id)
      (node-tasks-handler id))))

(rum/defc form-info < rum/static [{:keys [node tasks]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info (icon/os (:os node))
                 (:nodeName node))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :node-list)
          :label "Back"}))]]
   [:div.form-layout
    [:div.div.form-layout-group
     (form/section "General settings")
     (form/item "ID" (:id node))
     (form/item "NAME" (:nodeName node))
     (form/item "ROLE" (:role node))
     (form/item "OS" [(:os node) " " (:arch node)])
     (form/item "RESOURCES" (resources node))
     (form/item "ENGINE" ["docker " (:engine node)])
     (form/item "IP" (:address node))]
    [:div.form-layout-group.form-layout-group-border
     (form/section "Plugins")
     (form/item "NETWORK " (->> node :plugins :networks (interpose ", ")))
     (form/item "VOLUME" (->> node :plugins :volumes (interpose ", ")))]
    [:div.form-layout-group.form-layout-group-border
     (form/section "Status")
     (form/item "STATE" (:state node))
     (form/item "AVAILABILITY" (:availability node))
     (form/item "LEADER" (if (:leader node)
                           "yes"
                           "no"))]
    [:div.form-layout-group.form-layout-group-border
     (form/section "Linked Tasks")
     (list/table (map :name tasks/headers)
                 tasks
                 tasks/render-item
                 tasks/render-item-keys
                 tasks/onclick-handler)]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
