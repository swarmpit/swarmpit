(ns swarmpit.component.task.info
  (:require [material.component.label :as label]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [material.icon :as icon]
            [rum.core :as rum]))

(enable-console-print!)

(defn- task-handler
  [task-id]
  (ajax/get
    (routes/path-for-backend :task {:id task-id})
    {:progress   [:loading?]
     :on-success (fn [response]
                   (state/set-value response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (task-handler id))))

(rum/defc form-info < rum/static [{:keys [repository status] :as item}]
  (let [error (:error status)
        image-digest (:imageDigest repository)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/tasks
                   (:taskName item)
                   (label/info (:state item)))]]
     [:div.form-view
      [:div.form-view-group
       (form/section "General settings")
       (form/item "ID" (:id item))
       (form/item "NAME" (:taskName item))
       (form/item-date "CREATED" (:createdAt item))
       (form/item-date "LAST UPDATE" (:updatedAt item))
       (form/item "IMAGE" (get-in item [:repository :image]))
       (when (some? image-digest)
         (form/item "IMAGE DIGEST" image-digest))]
      [:div.form-view-group
       (form/section "Status")
       (form/item "STATE" (:state item))
       (form/item "DESIRED STATE" (:desiredState item))]
      (when (some? error)
        [:div.form-view-group
         (form/section "Error")
         (form/value error)])]]))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))