(ns swarmpit.component.registry.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/registries
                      (:name item))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:name item))
     (comp/form-item "VERSION" (:version item))
     (comp/form-item "URL" (str (:scheme item) "://" (:url item)))
     (comp/form-item "IS PRIVATE" (if (:isPrivate item)
                                    "yes"
                                    "no"))
     (if (:isPrivate item)
       [:div (comp/form-section "Registry access")
        (comp/form-item "USERNAME" (:username item))
        (comp/form-item "PASSWORD" (:password item))])]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
