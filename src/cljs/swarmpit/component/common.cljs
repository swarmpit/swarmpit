(ns swarmpit.component.common
  (:refer-clojure :exclude [list])
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(defn list-empty [title]
  (comp/typography
    {:key "scclcct"} (str "There are no " title " configured.")))

(defn list-no-items-found []
  (comp/typography
    {:key "scclccit"} "Nothing matches this filter."))

(rum/defc list-toolbar-filter-menu < rum/reactive [title filters]
  (let [anchorEl (state/react (conj state/form-state-cursor :listFilterAnchorEl))]
    (html
      [:div {:key "ltfm"}
       [:div {:className "Swarmpit-section-desktop"
              :key       "lffmsd"}
        (comp/button
          {:aria-owns     (when anchorEl "list-filter-menu")
           :aria-haspopup "true"
           :color         "primary"
           :key           "lfmbtn"
           :onClick       (fn [e]
                            (state/update-value [:listFilterAnchorEl] (.-currentTarget e) state/form-state-cursor))}
          (icon/filter-list {:className "Swarmpit-button-icon"
                             :key       "lfmbtnico"})
          "Filter")]
       [:div {:className "Swarmpit-section-mobile"
              :key       "lffmsm"}
        (comp/tooltip
          {:title "Filter"
           :key   "lfmitt"}
          (comp/icon-button
            {:aria-owns     (when anchorEl "list-filter-menu")
             :aria-haspopup "true"
             :key           "lfmibtn"
             :onClick       (fn [e]
                              (state/update-value [:listFilterAnchorEl] (.-currentTarget e) state/form-state-cursor))
             :color         "primary"}
            (icon/filter-list {})))]
       (comp/menu
         {:id              "list-filter-menu"
          :key             "lfm"
          :anchorEl        anchorEl
          :anchorOrigin    {:vertical   "top"
                            :horizontal "right"}
          :transformOrigin {:vertical   "top"
                            :horizontal "right"}
          :open            (some? anchorEl)
          :onClose         #(state/update-value [:listFilterAnchorEl] nil state/form-state-cursor)}
         (comp/menu-item
           {:key       "lfmi"
            :className "Swarmpit-menu-info"
            :disabled  true}
           (html [:span {:key "lfmicnt"}
                  (str "Filter " (str/lower-case title) " by")]))
         (map #(comp/menu-item
                 {:key      (str "mi-" (:name %))
                  :disabled (:disabled %)
                  :onClick  (:onClick %)}
                 (comp/form-control-label
                   {:control (comp/checkbox
                               {:key     (str "mifclcb-" (:name %))
                                :checked (:checked %)
                                :value   (str (:checked %))})
                    :key     (str "mifcl-" (:name %))
                    :label   (:name %)})) filters))])))

(rum/defc list-toobar < rum/reactive
  [title items filtered-items {:keys [actions filters] :as metadata}]
  (comp/mui
    (comp/toolbar
      {:key            "ltt"
       :disableGutters true
       :className      "Swarmpit-form-toolbar-context"}
      (comp/typography
        {:key     "lttt"
         :variant "subtitle1"
         :color   "inherit"
         :noWrap  false}
        (if (= (count items)
               (count filtered-items))
          (str "Total (" (count items) ")")
          (str "Total (" (count filtered-items) "/" (count items) ")")))
      (when actions
        (html
          [:div {:key   "ltavl"
                 :style {:borderRight "0.1em solid black"
                         :padding     "0.5em"
                         :height      0}}]))
      (when actions
        (map-indexed
          (fn [index action]
            (html
              [:div {:key (str "laml-" index)}
               [:div.Swarmpit-section-desktop
                (comp/button
                  {:color   "primary"
                   :key     (str "lambtn-" index)
                   :onClick (:onClick action)}
                  ((:icon action) {:className "Swarmpit-button-icon"})
                  (:name action))]
               [:div.Swarmpit-section-mobile
                ;; Make FAB from first only (primary action)
                (when (= 0 index)
                  (comp/button
                    {:variant   "fab"
                     :className "Swarmpit-fab"
                     :color     "primary"
                     :onClick   (:onClick action)}
                    ((:icon-alt action) {})))
                (comp/tooltip
                  {:title (:name action)
                   :key   (str "lamitt-" index)}
                  (comp/icon-button
                    {:key     (str "lamibtn-" index)
                     :onClick (:onClick action)
                     :color   "primary"}
                    ((:icon action) {})))]])) actions))
      (html [:div {:className "grow"
                   :key       "ltge"}])
      (when filters
        (list-toolbar-filter-menu title filters)))))

(rum/defc list < rum/reactive
  [title items filtered-items render-metadata onclick-handler toolbar-render-metadata]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div
        (list-toobar title items filtered-items toolbar-render-metadata)]
       [:div.Swarmpit-form-toolbar
        (cond
          (empty? items) (list-empty title)
          (empty? filtered-items) (list-no-items-found)
          :else
          (comp/card
            {:className "Swarmpit-card"
             :key       "scclc"}
            (comp/card-content
              {:className "Swarmpit-table-card-content"
               :key       "scclcc"}
              (rum/with-key
                (list/responsive
                  render-metadata
                  filtered-items
                  onclick-handler)
                "scclccrl"))))]])))

(rum/defc list-grid < rum/reactive
  [title items filtered-items grid toolbar-render-metadata]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div
        (list-toobar title items filtered-items toolbar-render-metadata)]
       [:div.Swarmpit-form-toolbar
        (cond
          (empty? items) (list-empty title)
          (empty? filtered-items) (list-no-items-found)
          :else grid)]])))

(defn show-password-adornment [show-password]
  (comp/input-adornment
    {:position "end"}
    (comp/icon-button
      {:aria-label  "Toggle password visibility"
       :onClick     #(state/update-value [:showPassword] (not show-password) state/form-state-cursor)
       :onMouseDown (fn [event]
                      (.preventDefault event))}
      (if show-password
        icon/visibility
        icon/visibility-off))))

(rum/defc action-menu-popper < rum/reactive [items-hash items anchorKey menuOpenKey]
  (let [moreAnchorEl (state/react (conj state/form-state-cursor anchorKey))
        menuOpen? (state/react (conj state/form-state-cursor menuOpenKey))]
    (comp/popper
      {:open          (or menuOpen? false)
       :anchorEl      moreAnchorEl
       :placement     "bottom-end"
       :className     "Swarmpit-popper"
       :disablePortal true
       :transition    true}
      (fn [props]
        (let [{:keys [TransitionProps placement]} (js->clj props :keywordize-keys true)]
          (comp/fade
            (merge TransitionProps
                   {:timeout 450})
            (comp/paper
              {:key (str "cmpop-" items-hash)}
              (comp/click-away-listener
                {:key         (str "cmpocal-" items-hash)
                 :onClickAway #(state/update-value [menuOpenKey] false state/form-state-cursor)}
                (comp/menu-list
                  {:key (str "cmml-" items-hash)}
                  (map
                    #(comp/menu-item
                       {:key      (str "cmmi-" items-hash "-" (:name %))
                        :disabled (:disabled %)
                        :onClick  (fn []
                                    ((:onClick %))
                                    (state/update-value [menuOpenKey] false state/form-state-cursor))}
                       (comp/list-item-icon
                         {:key (str "cmmii-" items-hash "-" (:name %))} (:icon %))
                       (comp/typography
                         {:variant "inherit"
                          :key     (str "cmmit-" items-hash "-" (:name %))} (:name %))) items))))))))))

(rum/defc action-menu-more < rum/static [type items-hash anchorKey menuOpenKey]
  (comp/icon-button
    {:key           (str "cmm-" type "-" items-hash)
     :aria-haspopup "true"
     :buttonRef     (fn [n]
                      (when n
                        (state/update-value [anchorKey] n state/form-state-cursor)))
     :onClick       (fn [e]
                      (state/update-value [menuOpenKey] true state/form-state-cursor))
     :color         "inherit"} icon/more))

(rum/defc actions-menu < rum/static [items anchorKey menuOpenKey]
  (let [generate-key (fn [k type] (keyword (str (name k) "-" type)))
        anchor-desktop-key (generate-key anchorKey "desktop")
        anchor-mobile-key (generate-key anchorKey "mobile")
        menu-open-desktop-key (generate-key menuOpenKey "desktop")
        menu-open-mobile-key (generate-key menuOpenKey "mobile")
        more-items-desktop (filter #(:more %) items)
        items-hash (hash items)]
    (html
      [:div
       [:div.Swarmpit-appbar-section-desktop
        (->> items
             (filter #(some? %))
             (filter #(and (nil? (:more %))
                           (or (nil? (:disabled %))
                               (false? (:disabled %)))))
             (map #(comp/tooltip
                     {:title (:name %)
                      :key   (str "cmt-" items-hash "-" (:name %))}
                     (comp/icon-button
                       {:color   "inherit"
                        :key     (str "cmb-" items-hash "-" (:name %))
                        :onClick (:onClick %)} (:icon %)))))
        (when (not-empty more-items-desktop)
          (action-menu-more "desktop" items-hash anchor-desktop-key menu-open-desktop-key))
        (action-menu-popper items-hash more-items-desktop anchor-desktop-key menu-open-desktop-key)]
       [:div.Swarmpit-appbar-section-mobile
        (action-menu-more "mobile" items-hash anchor-mobile-key menu-open-mobile-key)
        (action-menu-popper items-hash items anchor-mobile-key menu-open-mobile-key)]])))
