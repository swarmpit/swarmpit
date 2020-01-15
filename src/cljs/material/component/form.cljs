(ns material.component.form
  (:refer-clojure :exclude [comp])
  (:require [material.components :as cmp]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]
            [swarmpit.time :as time]))

(defn item-main
  ([name value]
   (item-main name value true))
  ([name value separator?]
   (cmp/box
     {:className "Swarmpit-form-item-wrapper"}
     (when separator?
       (cmp/divider {}))
     (cmp/box
       {:className "Swarmpit-form-item"}
       (cmp/box
         {:className "Swarmpit-form-item-name"}
         (cmp/typography {:variant "body2"} name))
       (cmp/box
         {:className "Swarmpit-form-item-value"}
         (cmp/typography {:variant "body2"} value))))))

(defn item-date
  [date]
  (html
    (if (= "0001-01-01T00:00:00Z" date)
      "sometime"
      [:time {:date-time date
              :title     (time/simplify date)}
       (time/humanize date)])))

(defn item [name value]
  (html
    [:div {:class "Swarmpit-row-space"
           :key   (str "sri-" name)}
     (cmp/typography {:variant "body2"
                      :color   "textSecondary"} name)
     (cmp/typography {:variant "body2"} value)]))

(defn item-info [message]
  (cmp/typography {:variant "body2"
                   :color   "textSecondary"} message))

(defn message [comp]
  (html
    [:span.Swarmpit-message
     (icon/info {:style {:marginRight "8px"}})
     [:span comp]]))

(defn error-message [text]
  (cmp/snackbar-content
    {:className "Swarmpit-label-red"
     :elevation 0
     :message   (html [:span.Swarmpit-message
                       (icon/error {:className "Swarmpit-message-icon"}) text])}))

(defn item-labels [labels]
  (html
    [:div {:class "Swarmpit-form-card-labels"
           :key   "item-labels"}
     labels]))

(defn section
  ([name]
   (section name nil))
  ([name button]
   (cmp/box
     {:class "Swarmpit-form-section"
      :id    name}
     (cmp/typography
       {:variant "h6"} name)
     button)))

(defn subsection
  ([name]
   (subsection name nil))
  ([name button]
   (cmp/box
     {:class "Swarmpit-form-section Swarmpit-form-subsection"
      :id    name}
     (cmp/typography
       {:variant "subtitle2"} name)
     button)))

(defn open-in-new [text href]
  (html
    [:a {:href      href
         :className "Swarmpit-new-tab"
         :target    "_blank"}
     [:div text]
     [:div (icon/open-in-new
             {:className "Swarmpit-new-tab-ico"})]]))