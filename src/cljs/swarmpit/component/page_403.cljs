(ns swarmpit.component.page-403
  (:require [swarmpit.routes :as routes]
            [rum.core :as rum]))

(rum/defc form < rum/static []
  [:div.page-back
   [:div.page
    [:div.page-form
     [:span
      [:h1 "403"]
      [:p "You are not authorized for selected action"]
      [:p "Go to login " [:a {:href (routes/path-for-frontend :login)} "page"]]]]]])
