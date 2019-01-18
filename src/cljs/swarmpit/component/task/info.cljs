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
            [rum.core :as rum]))

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
  [{:keys [id taskName nodeName state status createdAt updatedAt repository]}]
  (comp/card
    {:className "Swarmpit-form-card"
     :key       "tgc"}
    (comp/card-header
      {:title     taskName
       :key       "tgch"
       :className "Swarmpit-form-card-header"
       :subheader (form-subheader
                    (:image repository)
                    (:imageDigest repository))})
    (comp/card-content
      {:key "tgcc"}
      (html
        [:div {:key "tgccid"}
         [:span "Task is allocated to node " [:a {:href (routes/path-for-frontend :node-info {:id nodeName})} nodeName]]
         [:br]
         (when (:error status)
           [:p "Failure reason: " [:span (:error status)]])]))
    (comp/card-content
      {:key "tgccl"}
      (form/item-labels
        [(form-state state)]))
    (comp/divider
      {:key "tgd"})
    (comp/card-content
      {:style {:paddingBottom "16px"}
       :key   "tgccf"}
      (form/item-date createdAt updatedAt)
      (form/item-id id))))

(rum/defc form-info < rum/static [{:keys [repository status] :as item}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   16}
          (comp/grid
            {:item true
             :key  "tgg"
             :xs   12
             :sm   6}
            (section-general item)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
