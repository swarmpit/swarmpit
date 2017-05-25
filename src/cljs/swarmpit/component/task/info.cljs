(ns swarmpit.component.task.info
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:taskName item))
     (comp/form-item "CREATED" (:createdAt item))
     (comp/form-item "LAST UPDATE" (:updatedAt item))
     (comp/form-item "IMAGE" (:image item))
     (comp/form-item "IMAGE DIGEST" (:imageDigest item))
     (comp/form-section "Status")
     (comp/form-item "STATE" (:state item))
     (comp/form-item "DESIRED STATE" (:desiredState item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
