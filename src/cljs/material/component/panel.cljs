(ns material.component.panel
  (:require [material.component :as cmp]))

(defn text-field
  [props]
  (cmp/mui
    (cmp/text-field
      (merge props
             {:underlineStyle {:borderColor "rgba(0, 0, 0, 0.2)"}
              :style          {:height     "44px"
                               :lineHeight "15px"}}))))

(defn checkbox
  [props]
  (cmp/mui
    (cmp/checkbox
      (merge props
             {:style      {:width     "200px"
                           :marginTop "12px"}
              :labelStyle {:left -10}}))))

(defn info
  ([icon text]
   [:div.form-panel-info
    [:span.form-panel-info-icon
     (cmp/mui (cmp/svg icon))]
    [:span.form-panel-info-text text]])
  ([icon text state]
   [:div.form-panel-info
    [:span.form-panel-info-icon
     (cmp/mui (cmp/svg icon))]
    [:span.form-panel-info-text text]
    [:span.form-panel-info-label state]]))
