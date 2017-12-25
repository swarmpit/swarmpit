(ns swarmpit.component.node.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- node-tasks-handler
  [node-id]
  (handler/get
    (routes/path-for-backend :node-tasks {:id node-id})
    {:on-success (fn [response]
                   (state/update-value [:tasks] response cursor))}))

(defn- node-handler
  [node-id]
  (handler/get
    (routes/path-for-backend :node {:id node-id})
    {:on-success (fn [response]
                   (state/update-value [:node] response cursor))}))

(defn- init-state
  []
  (state/set-value {:secret   {}
                    :services []} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (node-handler id)
      (node-tasks-handler id))))

(rum/defc form-info < rum/static [node tasks]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/nodes
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
     (form/item "IP" (:address node))
     (form/item "ENGINE" (:engine node))]
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
  (let [{:keys [node tasks]} (state/react cursor)]
    (progress/form
      (empty? node)
      (form-info node tasks))))
