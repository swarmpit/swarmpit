(ns swarmpit.component.common
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn list-empty [title]
  (comp/typography
    {:key "scclcct"} (str "There are no " title " configured.")))

(defn list-no-items-found []
  (comp/typography
    {:key "scclccit"} "Nothing matches this filter."))

(defn list-toobar
  [title items {:keys [buttons] :as toolbar}]
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
        (str title " (" (count items) ")"))
      (when buttons
        (html
          [:div {:style {:borderRight "0.1em solid black"
                         :padding     "0.5em"
                         :height      0}}]))
      (when buttons
        buttons)
      (html [:div.grow])
      ;(comp/typography
      ;  {:key     "filter-label"
      ;   :variant "subtitle2"
      ;   :style   {:paddingRight "10px"}
      ;   :color   "inherit"
      ;   :noWrap  false}
      ;  "Filters: ")
      ;(comp/chip
      ;  {:onDelete   #(print "test")
      ;   :deleteIcon (icon/cancel {})
      ;   :style      {:marginRight "5px"}
      ;   :color      "primary"
      ;   :variant    "outlined"
      ;   :label      "Running"})
      ;(comp/chip
      ;  {:onDelete   #(print "test")
      ;   :deleteIcon (icon/cancel {})
      ;   :style      {:marginRight "5px"}
      ;   :color      "primary"
      ;   :variant    "outlined"
      ;   :label      "Something"})
      )))

(defn list
  [title items filtered-items render-metadata onclick-handler toolbar]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div
        (list-toobar title items toolbar)]
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

(rum/defc action-menu-popper < rum/reactive [hash items anchorKey menuOpenKey]
  (let [mobileMoreAnchorEl (state/react (conj state/form-state-cursor anchorKey))
        mobileMenuOpen? (state/react (conj state/form-state-cursor menuOpenKey))]
    (comp/popper
      {:open          (or mobileMenuOpen? false)
       :anchorEl      mobileMoreAnchorEl
       :placement     "bottom-end"
       :disablePortal true
       :transition    true}
      (fn [props]
        (let [{:keys [TransitionProps placement]} (js->clj props :keywordize-keys true)]
          (comp/fade
            (merge TransitionProps
                   {:timeout 450})
            (comp/paper
              {:key (str "cmpop-" hash "-" (:name %))}
              (comp/click-away-listener
                {:key         (str "cmpocal-" hash "-" (:name %))
                 :onClickAway #(state/update-value [menuOpenKey] false state/form-state-cursor)}
                (comp/menu-list
                  {:key (str "cmml-" hash "-" (:name %))}
                  (map
                    #(comp/menu-item
                       {:key      (str "cmmi-" hash "-" (:name %))
                        :disabled (:disabled %)
                        :onClick  (fn []
                                    ((:onClick %))
                                    (state/update-value [menuOpenKey] false state/form-state-cursor))}
                       (comp/list-item-icon
                         {:key (str "cmmii-" hash "-" (:name %))} (:icon %))
                       (comp/typography
                         {:variant "inherit"
                          :key     (str "cmmit-" hash "-" (:name %))} (:name %))) items))))))))))

(rum/defc action-menu-more < rum/static [hash anchorKey menuOpenKey]
  (comp/icon-button
    {:key           (str "cmm-" hash)
     :aria-haspopup "true"
     :buttonRef     (fn [n]
                      (when n
                        (state/update-value [anchorKey] n state/form-state-cursor)))
     :onClick       (fn [e]
                      (print "fdf")
                      (state/update-value [menuOpenKey] true state/form-state-cursor))
     :color         "inherit"} icon/more))

(rum/defc actions-menu < rum/static [items anchorKey menuOpenKey]
  (let [generate-key (fn [k type] (keyword (str (name k) "-" type)))
        anchor-desktop-key (generate-key anchorKey "desktop")
        anchor-mobile-key (generate-key anchorKey "mobile")
        menu-open-desktop-key (generate-key menuOpenKey "desktop")
        menu-open-mobile-key (generate-key menuOpenKey "mobile")
        more-items-desktop (filter #(:more %) items)
        has (hash items)]
    (html
      [:div
       [:div.Swarmpit-appbar-section-desktop
        (->> items
             (filter #(and (nil? (:more %))
                           (or (nil? (:disabled %))
                               (false? (:disabled %)))))
             (map #(comp/tooltip
                     {:title (:name %)
                      :key   (str "cmt-" has "-" (:name %))}
                     (comp/icon-button
                       {:color   "inherit"
                        :key     (str "cmb-" has "-" (:name %))
                        :onClick (:onClick %)} (:icon %)))))
        (when (not-empty more-items-desktop)
          (action-menu-more has anchor-desktop-key menu-open-desktop-key))
        (action-menu-popper has more-items-desktop anchor-desktop-key menu-open-desktop-key)]
       [:div.Swarmpit-appbar-section-mobile
        (action-menu-more has anchor-mobile-key menu-open-mobile-key)
        (action-menu-popper has items anchor-mobile-key menu-open-mobile-key)]])))
