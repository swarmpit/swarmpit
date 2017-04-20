(ns swarmpit.component.node.info
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel]
   [:div.form-view
    [:div.form-view-group
     (material/form-view-section "Node settings")
     (material/form-view-row "ID" (:id item))
     (material/form-view-row "NAME" (:name item))
     (material/form-view-row "STATUS" (:status item))
     (material/form-view-row "AVAILABILITY" (:availability item))
     (material/form-view-row "LEADER" (if (:leader item)
                                        "yes"
                                        "no"))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
