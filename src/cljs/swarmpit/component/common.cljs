(ns swarmpit.component.common
  (:refer-clojure :exclude [list])
  (:require [material.icon :as icon]
            [material.components :as comp :refer [current-theme-mode]]
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

(defn- instance-font-size [name]
  (let [len (count name)]
    (cond
      (<= len 8)  "1.5rem"
      (<= len 12) "1.25rem"
      (<= len 16) "1rem"
      :else       "0.85rem")))

(rum/defc title-logo < rum/static [instance-name]
  [:a.Swarmpit-title-link {:target "_blank"
                           :href   swarmpit-home-page}
   (if (and instance-name (not (clojure.string/blank? instance-name)))
     [:span.Swarmpit-title-custom
      [:img {:src    "img/icon.svg"
             :height "50"
             :width  "50"}]
      [:span.Swarmpit-title-instance
       {:style {:fontSize (instance-font-size instance-name)}}
       instance-name]]
     [:img {:src    "img/logo.svg"
            :height "50"
            :width  "200"}])])

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
           :color     "primary"} "Close"))
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
          (if (not= "" toolbar-render-metadata)
            (comp/grid
              {:item true
               :xs   12}
              (toolbar/list-toobar title items filtered-items toolbar-render-metadata)))
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
  (let [dark? (= "dark" (current-theme-mode))]
    (cond
      (< usage 75) {:name  "used"
                    :value usage
                    :hover value
                    :color (if dark? "#1b5e20" "#52B359")}
      (> usage 90) {:name  "used"
                    :value usage
                    :hover value
                    :color (if dark? "#c62828" "#d32f2f")}
      :else {:name  "used"
             :value usage
             :hover value
             :color (if dark? "#e65100" "#ffa000")})))

(rum/defc resource-pie < rum/reactive
  [{:keys [usage value limit type]} label id]
  (let [mode (rum/react comp/theme-mode)
        usage (or usage (* (/ value limit) 100))
        dark? (= "dark" mode)
        data [(resource-used usage value)
              {:name  "free"
               :value (- 100 usage)
               :hover (- limit value)
               :color (if dark? "#3a3a3a" "#ccc")}]]
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
          :color (if dark? "#3a3a3a" "#ccc")}]
        "Loading"
        "Swarmpit-stat-skeleton"
        id
        nil))))

(rum/defc resource-pie-empty < rum/reactive
  [id]
  (let [dark? (= "dark" (rum/react comp/theme-mode))]
    (chart/pie
      [{:value 100
        :color (if dark? "#3a3a3a" "#ccc")}]
      "-"
      "Swarmpit-stat-graph"
      id
      nil)))