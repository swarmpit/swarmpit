(ns swarmpit.component.node.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/info
                      (:nodeName item)
                      (comp/label-info (:state item)))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:nodeName item))
     (comp/form-item "AVAILABILITY" (:availability item))
     (comp/form-section "Status")
     (comp/form-item "STATE" (:state item))
     (comp/form-item "LEADER" (if (:leader item)
                                "yes"
                                "no"))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
