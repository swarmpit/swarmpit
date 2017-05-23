(ns swarmpit.component.task.info
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel]
   [:div.form-view
    [:div.form-view-group
     (comp/info-section "General settings")
     (comp/info-item "ID" (:id item))
     (comp/info-item "CREATED" (:createdAt item))
     (comp/info-item "LAST UPDATE" (:updatedAt item))
     (comp/info-item "IMAGE" (:image item))
     (comp/info-item "IMAGE DIGEST" (:imageDigest item))
     (comp/info-section "Status")
     (comp/info-item "STATE" (:state item))
     (comp/info-item "DESIRED STATE" (:desiredState item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
