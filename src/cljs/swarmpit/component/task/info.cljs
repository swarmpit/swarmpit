(ns swarmpit.component.task.info
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel]
   [:div.form-view
    [:div.form-view-group
     (material/form-view-section "General settings")
     (material/form-view-row "ID" (:id item))
     (material/form-view-row "CREATED" (:createdAt item))
     (material/form-view-row "LAST UPDATE" (:updatedAt item))
     (material/form-view-row "IMAGE" (:image item))
     (material/form-view-row "IMAGE DIGEST" (:imageDigest item))
     (material/form-view-section "Status")
     (material/form-view-row "STATE" (:state item))
     (material/form-view-row "DESIRED STATE" (:desiredState item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
