(ns swarmpit.component.header
  (:require [material.components :as comp]
            [material.icon :as icon]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.event.source :as eventsource]
            [swarmpit.component.state :as state]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [clojure.string :as string]
            [goog.crypt :as crypt])
  (:import [goog.crypt Md5]))

(enable-console-print!)

(defn- user-gravatar-hash [email]
  (let [md5 (Md5.)]
    (when (some? email)
      (do
        (.update md5 (string/trim email))
        (crypt/byteArrayToHex (.digest md5))))))

(defn- user-avatar []
  (comp/avatar
    {:className "Swarmpit-appbar-avatar"
     :src       (str "https://www.gravatar.com/avatar/"
                     (user-gravatar-hash (storage/email))
                     "?d=https://raw.githubusercontent.com/swarmpit/swarmpit/issues/159/resources/public/img/user.png")}))

(rum/defc user-info < rum/static []
  (html
    [:div
     [:span "Signed in as"]
     [:br]
     [:span.Swarmpit-appbar-user-menu-logged-user (storage/user)]]))

(rum/defc user-menu < rum/static [anchorEl]
  (html
    [:div
     (comp/icon-button
       {:aria-owns     (when anchorEl "Swarmpit-appbar-user-menu")
        :aria-haspopup "true"
        :onClick       (fn [e]
                         (state/update-value [:menuAnchorEl] (.-currentTarget e) state/layout-cursor))
        :color         "inherit"} (user-avatar))
     (comp/menu
       {:id              "Swarmpit-appbar-user-menu"
        :key             "appbar-user-menu"
        :anchorEl        anchorEl
        :anchorOrigin    {:vertical   "top"
                          :horizontal "right"}
        :transformOrigin {:vertical   "top"
                          :horizontal "right"}
        :open            (some? anchorEl)
        :onClose         #(state/update-value [:menuAnchorEl] nil state/layout-cursor)}
       (comp/menu-item
         {:key       "appbar-user-menu-logged-info"
          :className "Swarmpit-appbar-user-menu-logged-info"
          :disabled  true}
         (rum/with-key
           (user-info) "appbar-user-menu-sign-info"))
       (comp/divider
         {:key "appbar-user-menu-divider"})
       (comp/menu-item
         {:key     "appbar-user-menu-account-settings"
          :onClick (fn []
                     (state/update-value [:menuAnchorEl] nil state/layout-cursor)
                     (dispatch!
                       (routes/path-for-frontend :account-settings)))} "Settings")
       (comp/menu-item
         {:key     "appbar-user-menu-logout"
          :onClick (fn []
                     (storage/remove "token")
                     (eventsource/close!)
                     (dispatch!
                       (routes/path-for-frontend :login)))} "Log out"))]))

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

(defn- search-input [on-change-fn title]
  (html
    [:div.Swarmpit-appbar-search
     [:div.Swarmpit-appbar-search-icon icon/search]
     (comp/input
       {:placeholder      (str "Search " (string/lower-case title) " ...")
        :onChange         on-change-fn
        :fullWidth        true
        :classes          {:root  "Swarmpit-appbar-search-root"
                           :input "Swarmpit-appbar-search-input"}
        :id               "Swarmpit-appbar-search-filter"
        :key              "appbar-search"
        :disableUnderline true})]))

(defn- mobile-search-message [on-change-fn title]
  (html
    [:span#snackbar-mobile-search.Swarmpit-appbar-search-mobile-message
     (comp/input
       {:placeholder      (str "Search " (string/lower-case title) " ...")
        :onChange         on-change-fn
        :fullWidth        true
        :classes          {:root  "Swarmpit-appbar-search-mobile-root"
                           :input "Swarmpit-appbar-search-mobile-input"}
        :disableUnderline true})]))

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
          :color         "inherit"} icon/search))
     (when (some? actions)
       (comp/icon-button
         {:key           "menu-more"
          :aria-haspopup "true"
          :onClick       (fn [e]
                           (state/update-value [:mobileMoreAnchorEl] (.-currentTarget e) state/layout-cursor))
          :color         "inherit"} icon/more))]))

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

(rum/defc appbar < rum/reactive [{:keys [title subtitle search-fn actions]}]
  (let [{:keys [mobileSearchOpened menuAnchorEl mobileMoreAnchorEl]} (state/react state/layout-cursor)]
    (comp/mui
      (html
        [:div
         (comp/app-bar
           {:key       "appbar"
            :color     "primary"
            :className "Swarmpit-appbar"}
           (comp/toolbar
             {:key            "appbar-toolbar"
              :disableGutters false}
             (comp/icon-button
               {:key        "appbar-menu-btn"
                :color      "inherit"
                :aria-label "Open drawer"
                :onClick    #(state/update-value [:mobileOpened] true state/layout-cursor)
                :className  "Swarmpit-appbar-menu-btn"}
               icon/menu)
             (comp/typography
               {:key       "appbar-title"
                :className "Swarmpit-appbar-title"
                :variant   "h6"
                :color     "inherit"
                :noWrap    true}
               title)
             (comp/typography
               {:key       "appbar-subtitle"
                :className "Swarmpit-appbar-subtitle"
                :variant   "subtitle1"
                :color     "inherit"
                :noWrap    false}
               subtitle)
             (rum/with-key
               (html [:div.grow]) "appbar-grow")
             (rum/with-key
               (appbar-desktop-section search-fn actions title) "appbar-section-desktop")
             (rum/with-key
               (appbar-mobile-section search-fn actions) "appbar-section-mobile")
             (rum/with-key
               (user-menu menuAnchorEl) "appbar-section-user")))
         (mobile-actions-menu actions mobileMoreAnchorEl)
         (mobile-search search-fn title mobileSearchOpened)]))))
