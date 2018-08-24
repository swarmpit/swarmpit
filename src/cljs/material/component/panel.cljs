(ns material.component.panel
  (:require [material.component :as cmp]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]))

(defn search [text]
  (html
    [:div.Swarmpit-form-panel-search
     (cmp/form-control
       {:className "Swarmpit-form-panel-search"}
       (cmp/input-label
         {:htmlFor "input-with-search-adornment"} text)
       (cmp/input
         {:id             "input-with-search-adornment"
          :startAdornment (cmp/input-adornment
                            {:position "start"} icon/search)}))]))


(defn search-2 [text]
  (html
    [:div {:className "Swarmpit-form-panel-search"}

     [:div
      (cmp/input
        {:placeholder      text
         :fullWidth        true
         :className        "Swarmpit-form-panel-search-input"
         :disableUnderline true})]
     (cmp/icon-button
       {:className "Swarnpit-form-panel-search-icon"
        :disabled  true} icon/search)]))






;(defn text-field
;  [props]
;  (cmp/mui
;    (cmp/text-field
;      (merge props
;             {:underlineStyle {:borderColor "rgba(0, 0, 0, 0.2)"}
;              :style          {:height     "44px"
;                               :lineHeight "15px"}}))))
;
;(defn checkbox
;  [props]
;  (cmp/mui
;    (cmp/checkbox
;      (merge props
;             {:style      {:width     "200px"
;                           :marginTop "12px"}
;              :labelStyle {:left -10}}))))
;
;(defn info
;  ([icon text]
;   [:div.form-panel-info
;    [:span.form-panel-info-icon
;     (cmp/mui (cmp/svg icon))]
;    [:span.form-panel-info-text text]])
;  ([icon text state]
;   [:div.form-panel-info
;    [:span.form-panel-info-icon
;     (cmp/mui (cmp/svg icon))]
;    [:span.form-panel-info-text text]
;    [:span.form-panel-info-label state]]))
