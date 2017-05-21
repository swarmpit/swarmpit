(ns swarmpit.component.node.info
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
     (comp/form-view-row "NAME" (:name item))
     (comp/form-view-row "AVAILABILITY" (:availability item))
     (comp/form-view-section "Status")
     (comp/form-view-row "STATE" (:state item))
     (comp/form-view-row "LEADER" (if (:leader item)
                                    "yes"
                                    "no"))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
