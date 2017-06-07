(ns swarmpit.component.user.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/users
                      (:username item))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "USERNAME" (:username item))
     (comp/form-item "EMAIL" (:email item))
     (comp/form-item "IS ADMIN" (if (= "admin" (:role item))
                                  "yes"
                                  "no"))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
