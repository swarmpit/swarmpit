(ns swarmpit.component.task.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  (let [error (get-in item [:status :error])
        image-digest (get-in item [:repository :imageDigest])]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/tasks
                        (:taskName item)
                        (comp/label-info (:state item)))]]
     [:div.form-view
      [:div.form-view-group
       (comp/form-section "General settings")
       (comp/form-item "ID" (:id item))
       (comp/form-item "NAME" (:taskName item))
       (comp/form-item-date "CREATED" (:createdAt item))
       (comp/form-item-date "LAST UPDATE" (:updatedAt item))
       (comp/form-item "IMAGE" (get-in item [:repository :image]))
       (when (some? image-digest)
         (comp/form-item "IMAGE DIGEST" image-digest))]
      [:div.form-view-group
       (comp/form-section "Status")
       (comp/form-item "STATE" (:state item))
       (comp/form-item "DESIRED STATE" (:desiredState item))]
      (when (some? error)
        [:div.form-view-group
         (comp/form-section "Error")
         (comp/form-value error)])]]))