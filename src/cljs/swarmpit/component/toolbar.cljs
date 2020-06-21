(ns swarmpit.component.toolbar
  (:require [material.icon :as icon]
            [material.components :as comp]
            [sablono.core :refer-macros [html]]
            [clojure.string :refer [join]]
            [rum.core :as rum]))

(rum/defc menu-popper < rum/reactive [items-hash items anchor]
  (comp/popper
    {:open          (some? @anchor)
     :anchorEl      @anchor
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
            (comp/click-away-listener
              {:onClickAway #(reset! anchor nil)}
              (comp/menu-list
                (map
                  #(comp/menu-item
                     {:key      (str "cmmi-" items-hash "-" (:name %))
                      :disabled (:disabled %)
                      :onClick  (fn []
                                  ((:onClick %))
                                  (reset! anchor nil))}
                     (comp/list-item-icon
                       {:key       (str "cmmii-" items-hash "-" (:name %))
                        :className "Swarmpit-menu-icon"} (:icon %))
                     (comp/typography
                       {:variant "inherit"
                        :key     (str "cmmit-" items-hash "-" (:name %))} (:name %))) items)))))))))

(rum/defcs menu < rum/static
                  (rum/local nil :menu/anchor)
  [{anchor :menu/anchor} items]
  (let [items-hash (join (map :name items))
        main-action (first (filter #(= true (:main %)) items))
        rest-actions (filter #(not= true (:main %)) items)]
    (comp/box
      {}
      (comp/button-group
        {:className  "Swarmpit-form-toolbar-btn"
         :variant    "contained"
         :color      "primary"
         :aria-label "split button"}
        (comp/button
          {:startIcon (:icon main-action)
           :onClick   (:onClick main-action)}
          (:name main-action))
        (comp/button
          {:color         "primary"
           :size          "small"
           :aria-label    "select action"
           :aria-haspopup "menu"
           :onClick       #(reset! anchor (.-currentTarget %))}
          (icon/arrow-dropdown {})))
      (menu-popper items-hash rest-actions anchor))))

(rum/defc toolbar < rum/reactive [domain id actions]
  (let [group-actions (filter #(= true (:group %)) actions)
        single-actions (filter #(not= true (:group %)) actions)]
    (comp/mui
      (comp/toolbar
        {:disableGutters true
         :className      "Swarmpit-ftoolbar"}
        (comp/grid
          {:container  true
           :spacing    3
           :alignItems "flex-end"
           :justify    "space-between"}
          (comp/grid
            {:item true}
            (comp/box
              {:className "Swarmpit-ftoolbar-info"}
              (comp/typography
                {:variant   "h6"
                 :className "Swarmpit-ftoolbar-title"
                 :noWrap    false}
                domain)
              (comp/typography
                {:variant   "h6"
                 :className "Swarmpit-ftoolbar-subtitle"
                 :noWrap    false}
                id)))
          (comp/grid
            {:item true}
            (comp/box
              {:className "Swarmpit-ftoolbar-actions"}
              (when (not-empty group-actions)
                (menu group-actions))
              (when (not-empty single-actions)
                (map-indexed
                  (fn [index action]
                    (comp/button
                      (merge
                        {:color     (or (:color action) "primary")
                         :variant   (or (:variant action) "contained")
                         :key       (str "toolbar-button-" index)
                         :startIcon (:icon action)
                         :onClick   (:onClick action)}
                        (when (not= (dec (count single-actions)) index)
                          {:className "Swarmpit-form-toolbar-btn"}))
                      (:name action))) single-actions)))))))))

(rum/defc list-toobar < rum/reactive
  [title items filtered-items actions]
  (comp/mui
    (comp/toolbar
      {:disableGutters true
       :className      "Swarmpit-ftoolbar"}
      (comp/grid
        {:container  true
         :spacing    3
         :alignItems "flex-end"
         :justify    "space-between"}
        (comp/grid
          {:item true}
          (comp/box
            {:className "Swarmpit-ftoolbar-info"}
            (comp/typography
              {:variant   "subtitle1"
               :className "Swarmpit-ftoolbar-title"
               :noWrap    false}
              (if (= (count items)
                     (count filtered-items))
                (str "Total (" (count items) ")")
                (html [:span "Total (" [:b (count filtered-items)] "/" (count items) ")"])))))
        (comp/grid
          {:item true}
          (comp/box
            {:className "Swarmpit-ftoolbar-actions"}
            (when (not-empty actions)
              (map-indexed
                (fn [index action]
                  (comp/box
                    {:key (str "toolbar-item-" index)}
                    (comp/box
                      {}
                      (comp/button
                        (merge
                          {:color     (or (:color action) "primary")
                           :variant   (or (:variant action) "contained")
                           :key       (str "toolbar-button-" index)
                           :startIcon (:icon action)
                           :onClick   (:onClick action)}
                          (when (not= (dec (count actions)) index)
                            {:className "Swarmpit-form-toolbar-btn"}))
                        (:name action)))
                    (comp/box
                      {:className "Swarmpit-section-mobile"}
                      ;; Make FAB from first only (primary action)
                      (when (:primary action)
                        (comp/fab
                          {:className  "Swarmpit-fab"
                           :color      "primary"
                           :size       "large"
                           :aria-label "add"
                           :onClick    (:onClick action)}
                          (:icon-alt action)))))) actions))))))))