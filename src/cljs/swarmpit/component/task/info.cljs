(ns swarmpit.component.task.info
  (:require [material.components :as comp]
            [material.component.label :as label]
            [material.component.form :as form]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [material.component.chart :as chart]
            [clojure.contrib.inflect :as inflect]
            [clojure.contrib.humanize :as humanize]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(defonce digest-shown (atom false))

(defn- task-handler
  [task-id]
  (ajax/get
    (routes/path-for-backend :task {:id task-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (task-handler id))))

(defn form-state [value]
  (case value
    "preparing" (label/pulsing value)
    "starting" (label/pulsing value)
    "pending" (label/yellow value)
    "new" (label/blue value)
    "ready" (label/blue value)
    "assigned" (label/blue value)
    "accepted" (label/blue value)
    "complete" (label/blue value)
    "running" (label/green value)
    "shutdown" (label/grey value)
    "orphaned" (label/grey value)
    "rejected" (label/red value)
    "failed" (label/red value)))

(rum/defc form-subheader < rum/reactive [image image-digest]
  (if image-digest
    (comp/click-away-listener
      {:onClickAway #(reset! digest-shown false)}
      (comp/tooltip
        {:PopperProps          {:disablePortal true}
         :onClose              #(reset! digest-shown false)
         :open                 (rum/react digest-shown)
         :disableFocusListener true
         :disableHoverListener true
         :disableTouchListener true
         :title                image-digest}
        (html [:span {:onClick #(reset! digest-shown true)
                      :style   {:cursor "pointer"}} image])))
    (html [:span image])))

(defn- section-general
  [{:keys [id taskName nodeName state status createdAt updatedAt repository serviceName resources stats]}]
  (comp/card
    {:className "Swarmpit-form-card Swarmpit-form-card-single"}
    (comp/card-header
      {:title     taskName
       :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
       :subheader (form-subheader
                    (:image repository)
                    (:imageDigest repository))})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (html
        [:div
         (when stats
           [:div {:class "Swarmpit-node-stat"
                  :key   (str "node-card-stat-cpu")}
            (common/resource-pie
              (get-in stats [:cpuPercentage])
              (str (-> stats :cpuPercentage (Math/ceil)) "% cpu")
              (str "graph-cpu"))
            (common/resource-pie
              (get-in stats [:memoryPercentage])
              (str (humanize/filesize (-> stats :memory) :binary false) " ram")
              (str "graph-memory"))])]))
    (when (:error status)
      (comp/card-content
        {}
        (html [:span {:style {:color "#d32f2f"}} "Failure reason: " [:span (:error status)]])))
    (comp/card-content
      {}
      (form/item-labels
        [(form-state state)]))
    (comp/card-actions
      {}
      (comp/button
        {:size  "small"
         :color "primary"
         :href  (routes/path-for-frontend :service-info {:id serviceName})}
        "See service")
      (when nodeName
        (comp/button
          {:size  "small"
           :color "primary"
           :href  (routes/path-for-frontend :node-info {:id nodeName})}
          "See node"))
      (comp/button
        {:size  "small"
         :color "primary"
         :href  (routes/path-for-frontend :service-task-log {:id serviceName :taskId id})}
        "View log"))
    (comp/divider
      {})
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (form/item-date createdAt updatedAt)
      (form/item-id id))))

(rum/defc form-info < rum/static [{:keys [repository status] :as item}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (section-general item)]])))

(rum/defc form < rum/reactive
                 mixin/subscribe-form
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
