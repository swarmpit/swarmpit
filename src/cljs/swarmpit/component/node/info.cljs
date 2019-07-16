(ns swarmpit.component.node.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.contrib.humanize :as humanize]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]
            [clojure.contrib.inflect :as inflect]))

(enable-console-print!)

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(rum/defc form-general < rum/static [node]
  (let [cpu (-> node :resources :cpu (int))
        memory-bytes (-> node :resources :memory)
        disk-bytes (-> node :stats :disk :total)]
    (comp/card
      {:className "Swarmpit-form-card"}
      (comp/card-header
        {:title     (:nodeName node)
         :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
         :subheader (:address node)
         :action    (comp/tooltip
                      {:title "Edit node"}
                      (comp/icon-button
                        {:aria-label "Edit"
                         :href       (routes/path-for-frontend :node-edit {:id (:id node)})}
                        (comp/svg icon/edit-path)))})
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (html
          [:div
           [:div {:class "Swarmpit-node-stat"
                  :key   "node-card-stat-"}
            (common/resource-pie
              (get-in node [:stats :cpu :usedPercentage])
              (str cpu " " (inflect/pluralize-noun cpu "core"))
              "graph-cpu")
            (common/resource-pie
              (get-in node [:stats :disk :usedPercentage])
              (str (humanize/filesize disk-bytes :binary false) " disk")
              "graph-disk")
            (common/resource-pie
              (get-in node [:stats :memory :usedPercentage])
              (str (humanize/filesize memory-bytes :binary false) " ram")
              "graph-memory")]]))
      (comp/card-content
        {}
        (html [:span (str "docker engine " (:engine node)) " on " [(:os node) " " (:arch node)]]))
      (comp/card-content
        {}
        (form/item-labels
          [(node-item-state (:state node))
           (when (:leader node)
             (label/primary "Leader"))
           (label/grey (:role node))
           (if (= "active" (:availability node))
             (label/green "active")
             (label/grey (:availability node)))]))
      (comp/divider
        {})
      (comp/card-content
        {:style {:paddingBottom "16px"}}
        (form/item-id (:id node))))))

(def render-labels-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form-labels < rum/static [labels id]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Labels")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :node-edit {:id id}
                                   {:section "Labels"})}
                    (comp/svg icon/edit-path))})
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
         :title     (comp/typography {:variant "h6"} "Tasks")})
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

(defn form-general-grid [node]
  (comp/grid
    {:item true
     :xs   12}
    (form-general node)))

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

(rum/defc form-info < rum/static [id {:keys [node tasks]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
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
                (form-general-grid node)
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
                 :spacing   16}
                (form-task-grid tasks)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid node)
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
      (form-info id item))))
