(ns swarmpit.component.node.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.component.common :as common]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.contrib.humanize :as humanize]
            [clojure.contrib.inflect :as inflect]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- node-item-state [value]
  (case value
    "ready" (label/base value "green")
    "down" (label/base value "red")))

(defn- me-handler
  [node-id]
  (ajax/get
    (routes/path-for-backend :me)
    {:on-success (fn [{:keys [response]}]
                   (let [pinned-nodes (set (:node-dashboard response))]
                     (when (contains? pinned-nodes node-id)
                       (state/update-value [:pinned?] true state/form-state-cursor))))}))

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

(defn- delete-node-handler
  [node-id]
  (ajax/delete
    (routes/path-for-backend :node {:id node-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :node-list))
                   (message/info
                     (str "Node " node-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Node removal failed. " (:error response))))}))

(defn- pin-node-handler
  [node-id]
  (ajax/post
    (routes/path-for-backend :node-dashboard {:id node-id})
    {:on-success (fn [_]
                   (state/update-value [:pinned?] true state/form-state-cursor)
                   (message/info
                     (str "Node " node-id " pinned to dashboard.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Node pin failed. " (:error response))))}))

(defn- detach-node-handler
  [node-id]
  (ajax/delete
    (routes/path-for-backend :node-dashboard {:id node-id})
    {:on-success (fn [_]
                   (state/update-value [:pinned?] false state/form-state-cursor)
                   (message/info
                     (str "Node " node-id " detached to dashboard.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Node detach failed. " (:error response))))}))

(defn form-pin-action
  [id pinned? group?]
  (if pinned?
    {:onClick #(detach-node-handler id)
     :icon    (comp/svg icon/pin-path)
     :group   group?
     :name    "Detach"}
    {:onClick #(pin-node-handler id)
     :icon    (comp/svg icon/pin-path)
     :group   group?
     :name    "Pin"}))

(defn form-actions
  [id pinned?]
  [(form-pin-action id pinned? true)
   {:onClick #(dispatch! (routes/path-for-frontend :node-edit {:id id}))
    :icon    (comp/svg icon/edit-path)
    :main    true
    :group   true
    :name    "Edit"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :color   "default"
    :variant "outlined"
    :name    "Delete"}])

(rum/defc form-stats < rum/static [item]
  (let [cpu-usage (get-in item [:stats :cpu :usedPercentage])
        cpu-limit (get-in item [:stats :cpu :cores])
        disk (get-in item [:stats :disk :used])
        disk-usage (get-in item [:stats :disk :usedPercentage])
        disk-limit (get-in item [:stats :disk :total])
        memory (get-in item [:stats :memory :used])
        memory-usage (get-in item [:stats :memory :usedPercentage])
        memory-limit (get-in item [:stats :memory :total])]
    (if (= "down" (:state item))
      (comp/box
        {:className "Swarmpit-stat-empty"})
      (comp/box
        {:class "Swarmpit-stat"}
        (common/resource-pie
          {:value (* cpu-limit (/ cpu-usage 100))
           :limit cpu-limit
           :usage cpu-usage
           :type  :cpu}
          (str cpu-limit " vCPU")
          "graph-cpu")
        (common/resource-pie
          {:value disk
           :limit disk-limit
           :usage disk-usage
           :type  :disk}
          (str (common/render-capacity disk-limit false) " disk")
          "graph-disk")
        (common/resource-pie
          {:value memory
           :limit memory-limit
           :usage memory-usage
           :type  :memory}
          (str (common/render-capacity memory-limit true) " ram")
          "graph-memory")))))

(rum/defc form-general < rum/static [node pinned?]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:subheader (form/item-labels
                    [(node-item-state (:state node))
                     (when (:leader node)
                       (label/base "Leader" "primary"))
                     (label/base (:role node) "info")
                     (if (= "active" (:availability node))
                       (label/base "active" "green")
                       (label/base (:availability node) "info"))])})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (form-stats node))
    (form/item-main "ID" (:id node) false)
    (form/item-main "Name" (:nodeName node))
    (form/item-main "Address" (:address node))
    (form/item-main "Engine" (:engine node))
    (form/item-main "OS" (:os node))
    (form/item-main "Arch" (:arch node))))

(def render-labels-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form-labels < rum/static [labels id]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      (merge
        {:className "Swarmpit-table-card-header"
         :title     (comp/typography {:variant "h6"} "Labels")}))
    (if (empty? labels)
      (comp/card-content
        {}
        (html [:div "No labels defined for the node."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/list
          render-labels-metadata
          labels
          nil)))))

(rum/defc form-plugins < rum/static [networks volumes]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :title     (comp/typography {:variant "h6"} "Plugins")})
    (comp/card-content
      {:className "Swarmpit-form-card-content"}
      (form/subsection "Network")
      (map #(comp/chip
              {:label %
               :key   (str "plugin-" %)
               :style {:margin "5px 5px 5px 0px"}}) networks))
    (comp/card-content
      {:className "Swarmpit-form-card-content"}
      (form/subsection "Volume")
      (map #(comp/chip
              {:label %
               :key   (str "volume-" %)
               :style {:margin "5px 5px 5px 0px"}}) volumes))))

(rum/defc form-tasks < rum/static [tasks]
  (let [table-summary (->> (get-in tasks/render-metadata [:table :summary])
                           (filter #(not= "Node" (:name %)))
                           (into []))
        custom-metadata (assoc-in tasks/render-metadata [:table :summary] table-summary)]
    (comp/card
      {:className "Swarmpit-card"}
      (comp/card-header
        {:className "Swarmpit-table-card-header"
         :title     (comp/typography {:variant "h6"} "Tasks")
         :subheader (str "Running: " (count tasks))})
      (if (empty? tasks)
        (comp/card-content
          {}
          (html [:div "No tasks running on the node."]))
        (comp/card-content
          {:className "Swarmpit-table-card-content"}
          (list/responsive
            custom-metadata
            (filter #(not (= "shutdown" (:state %))) tasks)
            tasks/onclick-handler))))))

(defn- init-form-state
  []
  (state/set-value {:pinned?  false
                    :loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (node-handler id)
      (node-tasks-handler id)
      (me-handler id))))

(defn form-general-grid [node pinned?]
  (comp/grid
    {:item true
     :xs   12}
    (form-general node pinned?)))

(defn form-plugins-grid [networks volumes]
  (comp/grid
    {:item true
     :xs   12}
    (form-plugins networks volumes)))

(defn form-labels-grid [labels id]
  (comp/grid
    {:item true
     :xs   12}
    (form-labels labels id)))

(defn form-task-grid [tasks]
  (comp/grid
    {:item true
     :xs   12}
    (form-tasks tasks)))

(rum/defc form-info < rum/static [id {:keys [node tasks]} pinned?]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-node-handler id)
         "Delete node?"
         "Delete")
       [:div.Swarmpit-form-toolbar
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   2}
            (comp/grid
              {:item true
               :xs   12}
              (toolbar/toolbar
                "Node"
                (:nodeName node)
                (if (storage/admin?)
                  (form-actions id pinned?)
                  [(form-pin-action id pinned? false)])))
            (comp/grid
              {:item true
               :sm   6
               :md   4}
              (comp/grid
                {:container true
                 :spacing   2}
                (form-general-grid node pinned?)
                (form-plugins-grid
                  (->> node :plugins :networks)
                  (->> node :plugins :volumes))
                (form-labels-grid (:labels node) id)))
            (comp/grid
              {:item true
               :sm   6
               :md   8}
              (comp/grid
                {:container true
                 :spacing   2}
                (form-task-grid tasks)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   2}
            (comp/grid
              {:item true
               :xs   12}
              (toolbar/toolbar
                "Node"
                (:nodeName node)
                (if (storage/admin?)
                  (form-actions id pinned?)
                  [(form-pin-action id pinned? false)])))
            (form-general-grid node pinned?)
            (form-task-grid tasks)
            (form-plugins-grid
              (->> node :plugins :networks)
              (->> node :plugins :volumes))
            (form-labels-grid (:labels node) id)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [id]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info id item (:pinned? state)))))
