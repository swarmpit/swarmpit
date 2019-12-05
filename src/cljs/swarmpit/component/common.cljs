(ns swarmpit.component.common
  (:refer-clojure :exclude [list])
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [goog.string.format]
            [goog.string :as gstring]
            [rum.core :as rum]
            [material.component.chart :as chart]))

(def tooltip-shown (atom false))

(def swarmpit-home-page "https://swarmpit.io")

(defn- parse-version [version]
  (clojure.string/replace
    (:version version)
    #"SNAPSHOT"
    (->> (:revision version)
         (take 7)
         (apply str))))

(rum/defc title-logo < rum/static []
  [:a {:target "_blank"
       :href   swarmpit-home-page}
   [:img {:src    "img/swarmpit-transparent.png"
          :height "50"
          :width  "50"}]])

(rum/defc title-version < rum/static [version]
  (when version
    [:div.Swarmpit-title
     [:a {:target "_blank"
          :href   swarmpit-home-page}
      [:span.Swarmpit-title-name "Swarmpit"]]
     [:a {:target "_blank"
          :href   "/api-docs"}
      [:span.Swarmpit-title-version (parse-version version)]]]))

(rum/defc form-subheader < rum/reactive [subheader tooltip]
  (if subheader
    (comp/click-away-listener
      {:onClickAway #(reset! tooltip-shown false)}
      (comp/tooltip
        {:PopperProps          {:disablePortal true}
         :onClose              #(reset! tooltip-shown false)
         :open                 (rum/react tooltip-shown)
         :disableFocusListener true
         :disableHoverListener true
         :disableTouchListener true
         :title                tooltip}
        (html [:span {:onClick #(reset! tooltip-shown true)
                      :style   {:cursor "pointer"}} subheader])))
    (html [:span subheader])))

(defn form-title [title & subtitle]
  (html
    [:div.Swarmpit-form-title
     (comp/typography
       {:variant "h5"
        :key     "title"}
       title)
     (comp/typography
       {:variant "body2"
        :key     "subtitle"}
       subtitle)]))

(defn list-empty [title]
  (comp/typography
    {:key "empty-text"} (str "There are no " title " configured.")))

(defn list-no-items-found []
  (comp/typography
    {:key "nothing-match-text"} "Nothing matches this filter."))

(rum/defc list-toolbar-filter-menu < rum/reactive [title filters]
  (let [anchorEl (state/react (conj state/form-state-cursor :listFilterAnchorEl))]
    (html
      [:div
       [:div.Swarmpit-section-desktop
        (comp/button
          {:aria-owns     (when anchorEl "list-filter-menu")
           :className     "Swarmpit-form-toolbar-btn"
           :aria-haspopup "true"
           :color         "primary"
           :variant       "outlined"
           :onClick       (fn [e]
                            (state/update-value [:listFilterAnchorEl] (.-currentTarget e) state/form-state-cursor))}
          (icon/filter-list {:className "Swarmpit-button-icon"}) "Show Filters")]
       [:div.Swarmpit-section-mobile
        (comp/tooltip
          {:title "Show Filters"}
          (comp/icon-button
            {:aria-owns     (when anchorEl "list-filter-menu")
             :aria-haspopup "true"
             :onClick       (fn [e]
                              (state/update-value [:listFilterAnchorEl] (.-currentTarget e) state/form-state-cursor))
             :color         "primary"}
            (icon/filter-list {})))]
       (comp/menu
         {:id              "list-filter-menu"
          :anchorEl        anchorEl
          :anchorOrigin    {:vertical   "top"
                            :horizontal "right"}
          :transformOrigin {:vertical   "top"
                            :horizontal "right"}
          :open            (some? anchorEl)
          :onClose         #(state/update-value [:listFilterAnchorEl] nil state/form-state-cursor)}
         (comp/menu-item
           {:className "Swarmpit-menu-info"
            :disabled  true}
           (html [:span (str "Filter " (str/lower-case title) " by")]))
         (map #(comp/menu-item
                 {:key      (str "filter-" (:name %))
                  :disabled (:disabled %)
                  :onClick  (:onClick %)}
                 (comp/form-control-label
                   {:control (comp/checkbox
                               {:key     (str "filter-checkbox-" (:name %))
                                :checked (:checked %)
                                :color   "primary"
                                :value   (str (:checked %))})
                    :key     (str "filter-label-" (:name %))
                    :label   (:name %)})) filters))])))

(rum/defc list-toobar < rum/reactive
  [title items filtered-items {:keys [actions filters] :as metadata}]
  (comp/mui
    (comp/toolbar
      {:disableGutters true}
      (comp/typography
        {:variant "subtitle1"
         :color   "inherit"
         :noWrap  false}
        (if (= (count items)
               (count filtered-items))
          (str "Total (" (count items) ")")
          (str "Total (" (count filtered-items) "/" (count items) ")")))
      (html [:div.grow])
      (when actions
        (map-indexed
          (fn [index action]
            (html
              [:div {:key (str "toolbar-item-" index)}
               [:div.Swarmpit-section-desktop
                (comp/button
                  {:color     "primary"
                   :variant   "contained"
                   :key       (str "toolbar-button-" index)
                   :startIcon ((:icon action) {})
                   :onClick   (:onClick action)}
                  (:name action))]
               [:div.Swarmpit-section-mobile
                ;; Make FAB from first only (primary action)
                (when (= 0 index)
                  (comp/fab
                    {:className  "Swarmpit-fab"
                     :color      "primary"
                     :size       "large"
                     :aria-label "add"
                     :onClick    (:onClick action)}
                    ((:icon-alt action) {})))
                (comp/tooltip
                  {:title (:name action)
                   :key   (str "toolbar-tooltip-" index)}
                  (comp/icon-button
                    {:key     (str "toolbar-icon-btn-" index)
                     :onClick (:onClick action)
                     :color   "primary"}
                    ((:icon action) {})))]])) actions))
      (when filters
        (list-toolbar-filter-menu title filters)))))

(rum/defc list < rum/reactive
  [title items filtered-items render-metadata onclick-handler toolbar-render-metadata]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-toolbar
        (list-toobar title items filtered-items toolbar-render-metadata)
        (cond
          (empty? items) (list-empty title)
          (empty? filtered-items) (list-no-items-found)
          :else
          (comp/card
            {:className "Swarmpit-card"}
            (comp/card-content
              {:className "Swarmpit-table-card-content"}
              (list/responsive
                render-metadata
                filtered-items
                onclick-handler))))]])))

(rum/defc list-grid < rum/reactive
  [title items filtered-items grid toolbar-render-metadata]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-toolbar
        (list-toobar title items filtered-items toolbar-render-metadata)
        (cond
          (empty? items) (list-empty title)
          (empty? filtered-items) (list-no-items-found)
          :else grid)]])))

(rum/defc info-toobar < rum/reactive [domain id actions]
  (comp/mui
    (comp/toolbar
      {:disableGutters true}
      (comp/typography
        {:variant "h5"
         :noWrap  false}
        (str domain " #" (subs id 0 10)))
      (html [:div.grow])
      (when actions
        (map-indexed
          (fn [index action]
            (comp/button
              {:className "Swarmpit-form-toolbar-btn"
               :color     (or (:color action) "primary")
               :variant   (or (:variant action) "contained")
               :key       (str "toolbar-button-" index)
               :startIcon (:icon action)
               :onClick   (:onClick action)}
              (:name action))) actions)))))

(defn show-password-adornment
  ([show-password]
   (show-password-adornment show-password :showPassword))
  ([show-password password-key]
   (comp/input-adornment
     {:position "end"}
     (comp/icon-button
       {:aria-label  "Toggle password visibility"
        :onClick     #(state/update-value [password-key] (not show-password) state/form-state-cursor)
        :onMouseDown (fn [event]
                       (.preventDefault event))}
       (if show-password
         icon/visibility
         icon/visibility-off)))))

(defn tab-panel [{:keys [value index] :as props} childs]
  (comp/typography
    {:component       "div"
     :role            "tabpanel"
     :hidden          (not= value index)
     :id              (str "scrollable-auto-tabpanel-" index)
     :aria-labelledby (str "scrollable-auto-tab-" index)}
    (when (= value index)
      (comp/box {} childs))))

(defn resource-used [stat]
  (cond
    (< stat 75) {:name  "used"
                 :value stat
                 :color "#43a047"}
    (> stat 90) {:name  "used"
                 :value stat
                 :color "#d32f2f"}
    :else {:name  "used"
           :value stat
           :color "#ffa000"}))

(defn- render-percentage
  [val]
  (if (some? val)
    (str (gstring/format "%.2f" val) "%")
    "-"))

(rum/defc resource-pie < rum/static [stat label id]
  (let [data [(resource-used stat)
              {:name  "free"
               :value (- 100 stat)
               :color "#ccc"}]]
    (chart/pie
      data
      label
      "Swarmpit-node-stat-graph"
      id
      {:formatter (fn [value name props]
                    (render-percentage value))})))