(ns material.component.form
  (:refer-clojure :exclude [comp])
  (:require [material.component :as cmp]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]))

(defn item
  [name value]
  (cmp/grid
    {:container true
     :className "Swarmpit-form-item"}
    (cmp/grid
      {:item      true
       :xs        12
       :sm        6
       :className "Swarmpit-form-item-label"} name)
    (cmp/grid
      {:item true
       :xs   12
       :sm   6}
      value
      ;(cmp/typography
      ;  {:gutterBottom true
      ;   :noWrap       true
      ;   :variant      "body1"} value)
      )))

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

(defn form
  [items]
  (cmp/grid
    {:container true
     :direction "column"
     :xs        12
     :sm        6} items))

(defn subsection
  ([name]
   (subsection name nil))
  ([name button]
   (html [:div.Swarmpit-form-section
          [:div (cmp/typography {:variant "subheading"} name)]
          [:div button]])))

;(defn item-stack
;  [stack]
;  (when stack
;    (item " STACK " (html [:a {:href (routes/path-for-frontend :stack-info {:name stack})} stack]))))


;; Form components

;(defn checkbox
;  [props]
;  (cmp/checkbox
;    (merge props
;           {:style {:marginTop " 12px "}})))
;
;(defn toogle
;  [props]
;  (cmp/toogle
;    (merge props
;           {:style {:marginTop  " 12px "
;                    :marginLeft " -10px "}})))
;
;(defn loading [loading]
;  (let [mode (if loading " indeterminate "
;                         " determinate ")]
;    (cmp/mui
;      (cmp/linear-progress
;        {:mode  mode
;         :style {:borderRadius 0
;                 :background   " rgb (224, 228, 231) "
;                 :height       " 1px "}}))))
;
;(defn add-btn [label add-item-fn]
;  [:div.form-add-button
;   (cmp/mui
;     (cmp/flat-button
;       {:label         label
;        :labelPosition " before "
;        :primary       true
;        :onTouchTap    add-item-fn
;        :style         {:marginLeft " 10px "}
;        :icon          (cmp/svg icon/add-small)}))])

;; Form component layout

;(defn form [props & childs]
;  (cmp/mui
;    (cmp/vform props childs)))

;(defn comps [& comps]
;  (html comps))
;
;(defn comp [label comp]
;  (html
;    [:div.form-edit-row
;     [:span.form-row-label label]
;     [:div.form-row-field comp]]))
;
;(defn textarea [label textarea]
;  (html
;    [:div.form-edit-row
;     [:span.form-row-label label]
;     [:div.form-row-textarea textarea]]))

;(defn item [label value]
;  [:div.form-view-row
;   [:span.form-row-label label]
;   [:div.form-row-value value]])

;(defn value [value]
;  [:div.form-view-row
;   [:div.form-row-value value]])
;
;(defn icon-value [icon value]
;  [:div.form-view-row
;   [:div.form-row-icon-field (cmp/svg icon)]
;   [:div.form-row-value value]])

;(defn section [label]
;  [:div.form-view-row
;   [:span.form-row-section label]])

;(defn section-add
;  [label add-item-fn]
;  [:div.form-view-row
;   [:span.form-row-section.form-row-icon-section label]
;   [:div.form-row-icon-field
;    (cmp/mui
;      (cmp/svg
;        {:hoverColor " #437f9d "
;         :style      {:cursor " pointer "}
;         :onClick    #(add-item-fn)}
;        icon/add-small))]])

;(defn subsection [label]
;  [:div.form-view-row
;   [:span.form-row-subsection label]])

;(defn subsection-add
;  [label add-item-fn]
;  [:div.form-view-row
;   [:span.form-row-subsection.form-row-icon-section label]
;   [:div.form-row-icon-field
;    (cmp/mui
;      (cmp/svg
;        {:hoverColor " #437f9d "
;         :style      {:cursor    " pointer "
;                      :marginTop " 2px "
;                      :width     " 20px "
;                      :height    " 20px "}
;         :onClick    #(add-item-fn)}
;        icon/add-small))]])