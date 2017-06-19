(ns swarmpit.component.task.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  (let [error (get-in item [:status :error])]
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
       (comp/form-item "CREATED" (:createdAt item))
       (comp/form-item "LAST UPDATE" (:updatedAt item))
       (comp/form-item "IMAGE" (get-in item [:repository :image]))
       (comp/form-item "IMAGE DIGEST" (get-in item [:repository :imageDigest]))]
      [:div.form-view-group
       (comp/form-section "Status")
       (comp/form-item "STATE" (:state item))
       (comp/form-item "DESIRED STATE" (:desiredState item))]
      (if (some? error)
        [:div.form-view-group
         (comp/form-section "Error")
         (comp/form-icon-value icon/error error)])]]))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
