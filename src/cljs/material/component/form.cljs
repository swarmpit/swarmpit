(ns material.component.form
  (:refer-clojure :exclude [comp])
  (:require [material.component :as cmp]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]
            [swarmpit.time :as time]))

(defn item-date [created updated]
  (html
    [:div.Swarmpit-form-card-icon-item
     (icon/access-time
       {:className "Swarmpit-form-card-icon"})
     (when created
       [:time {:date-time created
               :title     (time/simplify created)}
        (str "created " (time/humanize created))])
     (when updated
       [:time {:date-time updated
               :title     (time/simplify updated)}
        (str (when created ", ") "last update " (time/humanize updated))])]))

(defn item-id [id]
  (html
    [:div.Swarmpit-form-card-icon-item
     (icon/fingerprint
       {:className "Swarmpit-form-card-icon"}) id]))

(defn item-labels [labels]
  (html
    [:div.Swarmpit-form-card-labels
     labels]))

(defn subheader [label icon]
  (html
    [:div.Swarmpit-form-card-subheader
     (icon
       {:className "Swarmpit-form-card-subheader-icon"}) label]))

(defn item-yn [value label]
  (html
    [:div.Swarmpit-form-card-icon-item
     (if value
       (icon/check
         {:className "Swarmpit-form-card-subheader-icon Swarmpit-label-green"})
       (icon/close
         {:className "Swarmpit-form-card-subheader-icon Swarmpit-label-red"})) label]))

(defn section
  ([name]
   (section name nil))
  ([name button]
   (html [:div.Swarmpit-form-section
          [:div (cmp/typography {:variant "h6"} name)]
          [:div button]])))

(defn subsection
  ([name]
   (subsection name nil))
  ([name button]
   (html [:div.Swarmpit-form-section
          [:div (cmp/typography {:variant "subtitle1"} name)]
          [:div button]])))