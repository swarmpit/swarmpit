(ns swarmpit.component.common
  (:refer-clojure :exclude [list])
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.chart :as chart]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.toolbar :as toolbar]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.humanize :as humanize]
            [clojure.contrib.inflect :as inflect]
            [goog.string.format]
            [goog.string :as gstring]
            [rum.core :as rum]))

(def swarmpit-home-page "https://swarmpit.io")

(defn parse-version [version]
  (clojure.string/replace
    (:version version)
    #"SNAPSHOT"
    (->> (:revision version)
         (take 7)
         (apply str))))

(rum/defc title-logo < rum/static []
  [:a {:target "_blank"
       :href   swarmpit-home-page}
   [:img {:src    "img/logo.svg"
          :height "50"
          :width  "200"}]])

(rum/defc title-version < rum/static [version]
  (when version
    [:span.Swarmpit-title-version (str "v" (parse-version version))]))

(rum/defc title-login-version < rum/static [version]
  (when version
    [:span.Swarmpit-login-version "Version: " (parse-version version)]))

(defn list-empty [title]
  (comp/typography
    {:key "empty-text"} (str "There are no " title " configured.")))

(defn list-no-items-found []
  (comp/typography
    {:key "nothing-match-text"} "Nothing matches this filter."))

(rum/defc list-filters < rum/static [filterOpen? comp]
  (comp/swipeable-drawer
    {:anchor "right"
     :open   filterOpen?}
    (comp/box
      {:className "Swarmpit-filter"}
      (comp/box
        {:className "Swarmpit-filter-actions"}
        (comp/button
          {:onClick   #(state/update-value [:filterOpen?] false state/form-state-cursor)
           :startIcon (icon/close {})
           :variant   "text"
           :color     "default"} "Close"))
      comp
      (comp/box {:className "grow"})
      (comp/button
        {:onClick   #(state/update-value [:filter] nil state/form-state-cursor)
         :startIcon (comp/svg icon/trash-path)
         :fullWidth true
         :variant   "contained"
         :color     "default"} "Clear"))))

(rum/defc list < rum/reactive
  [title items filtered-items render-metadata onclick-handler toolbar-render-metadata]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-toolbar
        (comp/grid
          {:container true
           :spacing   2}
          (comp/grid
            {:item true
             :xs   12}
            (toolbar/list-toobar title items filtered-items toolbar-render-metadata))
          (comp/grid
            {:item true
             :xs   12}
            (cond
              (empty? items) (list-empty title)
              (empty? filtered-items) (list-no-items-found)
              :else
              (comp/card
                {:className "Swarmpit-card"}
                (comp/card-content
                  {:className "Swarmpit-table-card-content"}
                  (list/responsive-footer
                    render-metadata
                    filtered-items
                    onclick-handler))))))]])))

(rum/defc list-grid < rum/reactive
  [title items filtered-items grid toolbar-render-metadata]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-toolbar
        (comp/grid
          {:container true
           :spacing   2}
          (comp/grid
            {:item true
             :xs   12}
            (toolbar/list-toobar title items filtered-items toolbar-render-metadata))
          (comp/grid
            {:item true
             :xs   12}
            (cond
              (empty? items) (list-empty title)
              (empty? filtered-items) (list-no-items-found)
              :else grid)))]])))

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
         (icon/visibility)
         (icon/visibility-off))))))

(defn tab-panel [{:keys [value index] :as props} & childs]
  (comp/typography
    {:component       "div"
     :role            "tabpanel"
     :hidden          (not= value index)
     :id              (str "scrollable-auto-tabpanel-" index)
     :aria-labelledby (str "scrollable-auto-tab-" index)}
    (when (= value index)
      (comp/box {} childs))))

(defn render-percentage
  [val]
  (if (some? val)
    (str (gstring/format "%.2f" val) "%")
    "-"))

(defn render-cores
  [val]
  (if (some? val)
    (gstring/format "%.2f" val)
    "-"))

(defn render-capacity
  [val binary?]
  (if (some? val)
    (humanize/filesize val :binary binary?)
    "-"))

(defn resource-used [usage value]
  (cond
    (< usage 75) {:name  "used"
                  :value usage
                  :hover value
                  :color "#52B359"}
    (> usage 90) {:name  "used"
                  :value usage
                  :hover value
                  :color "#d32f2f"}
    :else {:name  "used"
           :value usage
           :hover value
           :color "#ffa000"}))

(rum/defc resource-pie < rum/static
  [{:keys [usage value limit type]} label id]
  (let [usage (or usage (* (/ value limit) 100))
        data [(resource-used usage value)
              {:name  "free"
               :value (- 100 usage)
               :hover (- limit value)
               :color "#ccc"}]]
    (if limit
      (chart/pie
        data
        label
        "Swarmpit-stat-graph"
        id
        {:formatter (fn [value name props]
                      (let [hover-value (.-hover (.-payload props))]
                        (case type
                          :disk (render-capacity hover-value false)
                          :memory (render-capacity hover-value true)
                          :cpu (str (render-cores hover-value) " vCPU")
                          hover-value)))})
      (chart/pie
        [{:value 100
          :color "#ccc"}]
        "Loading"
        "Swarmpit-stat-skeleton"
        id
        nil))))

(rum/defc resource-pie-empty < rum/static
  [id]
  (chart/pie
    [{:value 100
      :color "#ccc"}]
    "-"
    "Swarmpit-stat-graph"
    id
    nil))