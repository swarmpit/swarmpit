(ns swarmpit.component.page-error
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.component :as comp]))

(defonce open (atom false))

(defn- open!
  []
  (reset! open true))

(defn- close!
  []
  (reset! open false))

(rum/defc form < rum/reactive [{{:keys [stacktrace]} :params}]
  [:div.page-back
   [:div.page
    [:div.page-form
     [:span
      [:h1 "500"]
      [:p "Ups something goes wrong..."]
      (comp/mui
        (comp/raised-button
          {:onClick open!
           :label   "Show stacktrace"
           :primary false}))
      (comp/mui
        (comp/dialog
          {:title                 "Stacktrace"
           :actions               (comp/flat-button
                                    {:onClick close!
                                     :label   "Cancel"
                                     :primary false})
           :open                  (rum/react open)
           :autoScrollBodyContent true
           :onRequestClose        close!
           :modal                 false}
          (html [:span.page-code
                 [:code stacktrace]])))]]]])