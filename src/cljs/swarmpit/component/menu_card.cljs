(ns swarmpit.component.menu_card
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc menu-popper < rum/reactive [items-hash items anchorKey menuOpenKey]
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

(rum/defc menu-more < rum/static [type items-hash anchorKey menuOpenKey]
  (comp/icon-button
    {:key           (str "cmm-" type "-" items-hash)
     :aria-haspopup "true"
     :buttonRef     (fn [n]
                      (when n
                        (state/update-value [anchorKey] n state/form-state-cursor)))
     :onClick       (fn [e]
                      (state/update-value [menuOpenKey] true state/form-state-cursor))
     :color         "inherit"} (icon/more)))

(rum/defc menu < rum/static [items anchorKey menuOpenKey]
  (let [generate-key (fn [k type] (keyword (str (name k) "-" type)))
        anchor-desktop-key (generate-key anchorKey "desktop")
        anchor-mobile-key (generate-key anchorKey "mobile")
        menu-open-desktop-key (generate-key menuOpenKey "desktop")
        menu-open-mobile-key (generate-key menuOpenKey "mobile")
        more-items-desktop (filter #(:more %) items)
        items-hash (hash items)]
    (html
      [:div
       [:div.Swarmpit-action-menu-desktop
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
          (menu-more "desktop" items-hash anchor-desktop-key menu-open-desktop-key))
        (menu-popper items-hash more-items-desktop anchor-desktop-key menu-open-desktop-key)]
       [:div.Swarmpit-action-menu-mobile
        (menu-more "mobile" items-hash anchor-mobile-key menu-open-mobile-key)
        (menu-popper items-hash items anchor-mobile-key menu-open-mobile-key)]])))