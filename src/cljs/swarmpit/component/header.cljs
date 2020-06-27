(ns swarmpit.component.header
  (:require [material.components :as comp]
            [material.icon :as icon]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.event.source :as eventsource]
            [swarmpit.component.common :as common]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [clojure.string :as string])
  (:import [goog.crypt Md5]))

(enable-console-print!)

(defn menu-badge [initials]
  (comp/badge
    {:classes      {:badge "Swarmpit-appbar-avatar-badge"}
     :overlap      "circle"
     :anchorOrigin {:vertical   "bottom"
                    :horizontal "right"}
     :variant      "dot"}
    (comp/avatar initials)))

(defn menu-dropdown []
  (icon/expand-more
    {:color     "primary"
     :className "Swarmpit-appbar-dropdown-icon"}))

(defn menu-user [username]
  (comp/card-content
    (comp/typography
      {:variant "body2"
       :color   "textSecondary"} "Username")
    (comp/typography
      {:variant "body1"} username)))

(defn admin-menu [open]
  (comp/collapse
    {:in            @open
     :timeout       "auto"
     :unmountOnExit true}
    (comp/menu-item
      {:className      "nested"
       :disablePadding true
       :onClick        (fn []
                         (state/update-value [:menuAnchorEl] nil state/layout-cursor)
                         (dispatch!
                           (routes/path-for-frontend :user-list)))}
      (comp/list-item-icon
        {:className "Swarmpit-appbar-menu-icon"}
        (comp/svg {:fontSize "small"} icon/users-path))
      (comp/list-item-text
        {:primary "Users"}))))

(rum/defcs menu < rum/static
                  (rum/local false :admin/open)
  [{open :admin/open}]
  (comp/menu-list
    {:disablePadding true}
    (comp/menu-item
      {:button  true
       :onClick (fn []
                  (state/update-value [:menuAnchorEl] nil state/layout-cursor)
                  (dispatch!
                    (routes/path-for-frontend :account-settings)))}
      (comp/list-item-icon
        {:className "Swarmpit-appbar-menu-icon"}
        (icon/settings {:fontSize "small"}))
      (comp/list-item-text
        {:primary "Settings"}))
    (when (storage/admin?)
      (comp/menu-item
        {:button  true
         :onClick #(reset! open (not @open))}
        (comp/list-item-icon
          {:className "Swarmpit-appbar-menu-icon"}
          (icon/supervisor-account {:fontSize "small"}))
        (comp/list-item-text
          {:primary "Admin"})
        (if @open
          (icon/expand-less)
          (icon/expand-more))))
    (admin-menu open)
    (comp/link
      {:href   "https://github.com/swarmpit/swarmpit/issues/new"
       :color  "inherit"
       :target "_blank"}
      (comp/menu-item
        {:button true}
        (comp/list-item-icon
          {:className "Swarmpit-appbar-menu-icon"}
          (icon/error-out {:fontSize "small"}))
        (comp/list-item-text
          {:primary "Report issue"})))
    (comp/divider)
    (comp/menu-item
      {:button  true
       :onClick (fn []
                  (storage/remove "token")
                  (eventsource/close!)
                  (state/update-value [:menuAnchorEl] nil state/layout-cursor)
                  (dispatch!
                    (routes/path-for-frontend :login)))}
      (comp/list-item-icon
        {:className "Swarmpit-appbar-menu-icon"}
        (icon/exit {:fontSize "small"}))
      (comp/list-item-text
        {:primary "Sign out"}))))

(defn badge [username]
  (when username
    (->> (string/split username #" ")
         (map #(get % 0))
         (string/join))))

(rum/defc user-menu < rum/static [anchorEl]
  (comp/box
    (comp/button
      {:className "PnsAppBar-user"
       :onClick   (fn [e]
                    (state/update-value [:menuAnchorEl] (.-currentTarget e) state/layout-cursor))
       :classes   {:endIcon "Swarmpit-appbar-dropdown"}
       :startIcon (menu-badge (badge (storage/user)))
       :endIcon   (menu-dropdown)} "")
    (comp/popper
      {:open          (some? anchorEl)
       :className     "Swarmpit-appbar-popper"
       :anchorEl      anchorEl
       :placement     "bottom-end"
       :disablePortal true
       :transition    true}
      (fn [_]
        (comp/paper
          (comp/click-away-listener
            {:onClickAway #(state/update-value [:menuAnchorEl] nil state/layout-cursor)}
            (comp/card
              (menu-user (storage/user))
              (comp/divider)
              (menu))))))))

(defn- mobile-actions-menu
  [actions mobileMoreAnchorEl]
  (comp/menu
    {:id              "Swarmpit-appbar-action-menu"
     :key             "Swarmpit-appbar-action-menu"
     :anchorEl        mobileMoreAnchorEl
     :anchorOrigin    {:vertical   "top"
                       :horizontal "right"}
     :transformOrigin {:vertical   "top"
                       :horizontal "right"}
     :open            (some? mobileMoreAnchorEl)
     :onClose         #(state/update-value [:mobileMoreAnchorEl] nil state/layout-cursor)}
    (->> actions
         (map #(comp/menu-item
                 {:key      (str "mobile-menu-item-" (:name %))
                  :disabled (:disabled %)
                  :onClick  (fn []
                              ((:onClick %))
                              (state/update-value [:mobileMoreAnchorEl] nil state/layout-cursor))}
                 (comp/list-item-icon
                   {:key (str "mobile-menu-item-icon-" (:name %))} (:icon %))
                 (comp/typography
                   {:variant "inherit"
                    :key     (str "mobile-menu-item-text-" (:name %))} (:name %)))))))

(rum/defc search-input < rum/reactive [on-change-fn title]
  (let [{:keys [query]} (state/react state/search-cursor)]
    (html
      [:div.Swarmpit-appbar-search
       [:div.Swarmpit-appbar-search-icon (icon/search {})]
       (comp/input
         {:placeholder      (str "Search " (string/lower-case title) " ...")
          :onChange         on-change-fn
          :defaultValue     query
          :type             "search"
          :fullWidth        true
          :classes          {:root  "Swarmpit-appbar-search-root"
                             :input "Swarmpit-appbar-search-input"}
          :id               "Swarmpit-appbar-search-filter"
          :key              "appbar-search"
          :disableUnderline true})])))

(rum/defc mobile-search-message < rum/reactive [on-change-fn title]
  (let [{:keys [query]} (state/react state/search-cursor)]
    (html
      [:span#snackbar-mobile-search.Swarmpit-appbar-search-mobile-message
       (comp/input
         {:placeholder      (str "Search " (string/lower-case title) " ...")
          :onChange         on-change-fn
          :defaultValue     query
          :type             "search"
          :fullWidth        true
          :classes          {:root  "Swarmpit-appbar-search-mobile-root"
                             :input "Swarmpit-appbar-search-mobile-input"}
          :disableUnderline true})])))

(defn- mobile-search-action []
  (html
    [:span
     (comp/icon-button
       {:onClick    #(state/update-value [:mobileSearchOpened] false state/layout-cursor)
        :key        "search-btn-close"
        :aria-label "Close"
        :color      "inherit"}
       (icon/close
         {:className "Swarmpit-appbar-search-mobile-close"}))]))

(defn- mobile-search [on-change-fn title opened]
  (comp/snackbar
    {:open         opened
     :anchorOrigin {:vertical   "top"
                    :horizontal "center"}
     :className    "Swarmpit-appbar-search-mobile"
     :onClose      #(state/update-value [:mobileSearchOpened] false state/layout-cursor)}
    (comp/snackbar-content
      {:aria-describedby "snackbar-mobile-search"
       :className        "Swarmpit-appbar-search-mobile-content"
       :classes          {:message "Swarmpit-appbar-search-mobile-content-message"}
       :message          (mobile-search-message on-change-fn title)
       :action           (mobile-search-action)})))

(rum/defc appbar-mobile-section < rum/static [search-fn actions]
  (html
    [:div.Swarmpit-appbar-section-mobile
     (when search-fn
       (comp/icon-button
         {:key           "menu-search"
          :aria-haspopup "true"
          :onClick       #(state/update-value [:mobileSearchOpened] true state/layout-cursor)
          :color         "inherit"} (icon/search {})))
     (when (some? actions)
       (comp/icon-button
         {:key           "menu-more"
          :aria-haspopup "true"
          :onClick       (fn [e]
                           (state/update-value [:mobileMoreAnchorEl] (.-currentTarget e) state/layout-cursor))
          :color         "inherit"} (icon/more)))]))

(rum/defc appbar-desktop-section < rum/static [search-fn actions title]
  (html
    [:div.Swarmpit-appbar-section-desktop
     (when search-fn
       (search-input search-fn title))
     (->> actions
          (filter #(or (nil? (:disabled %))
                       (false? (:disabled %))))
          (map #(comp/tooltip
                  {:title (:name %)
                   :key   (str "menu-tooltip-" (:name %))}
                  (comp/icon-button
                    {:color   "inherit"
                     :key     (str "menu-btn-" (:name %))
                     :onClick (:onClick %)} (:icon %)))))]))

(defonce appbar-elevation (atom 0))

(def mixin-on-scroll
  {:did-mount
   (fn [state]
     (.addEventListener
       js/window
       "scroll"
       (fn [_]
         (let [top? (zero? (-> js/window .-scrollY))]
           (if top?
             (reset! appbar-elevation 0)
             (reset! appbar-elevation 4))))) state)})

(rum/defc appbar < rum/reactive
                   mixin-on-scroll [{:keys [title subtitle search-fn actions]}]
  (let [{:keys [mobileSearchOpened menuAnchorEl mobileMoreAnchorEl version]} (state/react state/layout-cursor)
        elevation (rum/react appbar-elevation)]
    (comp/mui
      (html
        [:div
         (comp/app-bar
           {:key       "appbar"
            :color     "primary"
            :elevation elevation
            :id        "Swarmpit-appbar"
            :className "Swarmpit-appbar"}
           (comp/toolbar
             {:key            "appbar-toolbar"
              :disableGutters false}
             (html
               [:div.Swarmpit-desktop-title
                (common/title-logo)
                (common/title-version version)])
             (comp/icon-button
               {:key        "appbar-menu-btn"
                :color      "inherit"
                :aria-label "Open drawer"
                :onClick    #(state/update-value [:mobileOpened] true state/layout-cursor)
                :className  "Swarmpit-appbar-menu-btn"}
               (icon/menu))
             (comp/typography
               {:key       "appbar-title"
                :className "Swarmpit-appbar-title"
                :variant   "h6"
                :color     "inherit"
                :noWrap    true}
               title)
             (html [:div.grow])
             (appbar-desktop-section search-fn actions title)
             (appbar-mobile-section search-fn actions)
             (user-menu menuAnchorEl)))
         (mobile-actions-menu actions mobileMoreAnchorEl)
         (mobile-search search-fn title mobileSearchOpened)]))))
