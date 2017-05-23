(ns swarmpit.component.node.info
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
     (comp/info-item "NAME" (:name item))
     (comp/info-item "AVAILABILITY" (:availability item))
     (comp/info-section "Status")
     (comp/info-item "STATE" (:state item))
     (comp/info-item "LEADER" (if (:leader item)
                                    "yes"
                                    "no"))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
