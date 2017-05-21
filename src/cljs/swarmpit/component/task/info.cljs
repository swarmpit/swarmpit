(ns swarmpit.component.task.info
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel]
   [:div.form-view
    [:div.form-view-group
     (comp/form-view-section "General settings")
     (comp/form-view-row "ID" (:id item))
     (comp/form-view-row "CREATED" (:createdAt item))
     (comp/form-view-row "LAST UPDATE" (:updatedAt item))
     (comp/form-view-row "IMAGE" (:image item))
     (comp/form-view-row "IMAGE DIGEST" (:imageDigest item))
     (comp/form-view-section "Status")
     (comp/form-view-row "STATE" (:state item))
     (comp/form-view-row "DESIRED STATE" (:desiredState item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
