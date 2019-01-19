(ns material.component.form
  (:refer-clojure :exclude [comp])
  (:require [material.components :as cmp]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]
            [swarmpit.time :as time]))

(defn item [name value]
  (html
    [:div {:class "Swarmpit-row-space"
           :key   (str "sri-" name)}
     [:span name]
     [:span value]]))

(defn item-date [created updated]
  (html
    [:div {:class "Swarmpit-form-card-icon-item"}
     (icon/access-time
       {:className "Swarmpit-form-card-icon"})
     (when created
       [:time {:date-time created
               :title     (time/simplify created)}
        (cmp/typography
          {:color     "textSecondary"
           :className "Swarmpit-form-card-icon-text"}
          (str "created " (time/humanize created)))])
     (when updated
       [:time {:date-time updated
               :title     (time/simplify updated)}
        (cmp/typography
          {:color     "textSecondary"
           :className "Swarmpit-form-card-icon-text"}
          (str (when created ", ") "last update " (time/humanize updated)))])]))

(defn item-id [id]
  (html
    [:div {:class "Swarmpit-form-card-icon-item"}
     (icon/fingerprint
       {:className "Swarmpit-form-card-icon"})
     [:span.Swarmpit-form-card-icon-item-id
      (cmp/typography
        {:color     "textSecondary"
         :className "Swarmpit-form-card-icon-text"} id)]]))

(defn item-labels [labels]
  (html
    [:div {:class "Swarmpit-form-card-labels"
           :key   "item-labels"}
     labels]))

(defn section
  ([name]
   (section name nil))
  ([name button]
   (html
     [:div {:class "Swarmpit-form-section"
            :id    name}
      [:div
       (cmp/typography
         {:variant "h6"} name)]
      [:div button]])))

(defn subsection
  ([name]
   (subsection name nil))
  ([name button]
   (html
     [:div.Swarmpit-form-section
      [:div
       (cmp/typography
         {:variant "subtitle1"} name)]
      [:div button]])))

(defn open-in-new [text href]
  (html
    [:a {:href      href
         :className "Swarmpit-new-tab"
         :target    "_blank"}
     [:div text]
     [:div (icon/open-in-new
             {:className "Swarmpit-new-tab-ico"})]]))