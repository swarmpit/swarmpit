(ns swarmpit.component.layout
  (:require [rum.core :as rum]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.header :as header]))

(enable-console-print!)

(rum/defc layout < rum/reactive []
  (let [opened (rum/react menu/opened?)
        layout-class (if (true? opened)
                       "layout-opened"
                       "layout-closed")]
    [:div {:class ["layout" layout-class]}
     [:header (header/appbar)]
     [:nav (menu/drawer)]
     [:main [:div#content]]]))

(defn mount!
  []
  (rum/mount (layout) (.getElementById js/document "layout")))