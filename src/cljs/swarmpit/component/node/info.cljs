(ns swarmpit.component.node.info
  (:require [material.icon :as icon]
            [material.component :as comp]
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

(defn section-general [node]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     (:nodeName node)
       :className "Swarmpit-form-card-header"
       :subheader (:address node)})
    (comp/card-content
      {}
      (html
        [:div
         [:span "ENGINE: " (str "docker " (:engine node))]
         [:br]
         [:span "OS: " [(:os node) " " (:arch node)]]
         [:br]
         [:span "RESOURCES: " (resources node)]]))
    (comp/card-content
      {}
      (html
        [:div
         [:span "Network plugins: " (->> node :plugins :networks (interpose ", "))]
         [:br]
         [:span "Volume plugins: " (->> node :plugins :volumes (interpose ", "))]]))
    (comp/card-content
      {}
      (form/item-labels
        [(when (:leader node)
           (label/grey "Leader"))
         (label/grey (:state node))
         (label/grey (:availability node))
         (label/grey (:role node))]))
    (comp/divider)
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (comp/typography
        {:color "textSecondary"
         :style {:flexDirection "column"}}
        (form/item-id (:id node))))))

(def render-labels-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(defn section-labels [labels]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Labels"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/list
        render-labels-metadata
        labels
        nil))))

(defn section-tasks [tasks]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Tasks"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        tasks/render-metadata
        tasks
        tasks/onclick-handler))))

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

(defn form-actions
  [{:keys [params]}]
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch!
                           (routes/path-for-frontend :node-edit {:id (:id params)}))}
              (comp/svg icon/edit))
    :name   "Edit"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (node-handler id)
      (node-tasks-handler id))))

(rum/defc form-info < rum/static [id {:keys [node tasks]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   40}
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (section-general node))
          (when (not-empty (:labels node))
            (comp/grid
              {:item true
               :xs   12
               :sm   6}
              (section-labels (:labels node))))
          (when (not-empty tasks)
            (comp/grid
              {:item true
               :xs   12}
              (section-tasks tasks))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [id]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info id item))))
