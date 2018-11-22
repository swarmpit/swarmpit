(ns swarmpit.component.header
  (:require [material.component :as comp]
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

(defn- user-menu [anchorEl]
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
        :key             "Swarmpit-appbar-user-menu"
        :anchorEl        anchorEl
        :anchorOrigin    {:vertical   "top"
                          :horizontal "right"}
        :transformOrigin {:vertical   "top"
                          :horizontal "right"}
        :open            (some? anchorEl)
        :onClose         #(state/update-value [:menuAnchorEl] nil state/layout-cursor)}
       (comp/menu-item
         {:key       "Swarmpit-appbar-user-menu-logged-info"
          :className "Swarmpit-appbar-user-menu-logged-info"
          :disabled  true} (html [:div
                                  [:span "Signed in as"]
                                  [:br]
                                  [:span.Swarmpit-appbar-user-menu-logged-user (storage/user)]]))
       (comp/divider)
       (comp/menu-item
         {:key     "Swarmpit-appbar-user-menu-api-token"
          :onClick (fn []
                     (dispatch!
                       (routes/path-for-frontend :api-access)))} "API access")
       (comp/menu-item
         {:key     "Swarmpit-appbar-user-menu-settings"
          :onClick (fn []
                     (dispatch!
                       (routes/path-for-frontend :password)))} "Change password")
       (comp/menu-item
         {:key     "Swarmpit-appbar-user-menu-logout"
          :onClick (fn []
                     (storage/remove "token")
                     (eventsource/close!)
                     (dispatch!
                       (routes/path-for-frontend :login)))} "Log out"))]))

(defn- mobile-actions-menu
  [actions mobileMoreAnchorEl]
  (comp/menu
    {:anchorEl        mobileMoreAnchorEl
     :anchorOrigin    {:vertical   "top"
                       :horizontal "right"}
     :transformOrigin {:vertical   "top"
                       :horizontal "right"}
     :open            (some? mobileMoreAnchorEl)
     :onClose         #(state/update-value [:mobileMoreAnchorEl] nil state/layout-cursor)}
    (->> actions
         (map #(comp/menu-item
                 {}
                 (:button %)
                 (html [:p (:name %)]))))))

(defn- search-input [on-change-fn]
  (html
    [:div.Swarmpit-appbar-search
     [:div.Swarmpit-appbar-search-icon icon/search]
     (comp/input
       {:placeholder      "Search..."
        :onChange         on-change-fn
        :fullWidth        true
        :classes          {:root  "Swarmpit-appbar-search-root"
                           :input "Swarmpit-appbar-search-input"}
        :id               "Swarmpit-appbar-search-filter"
        :disableUnderline true})]))

(defn- mobile-search-message [on-change-fn]
  (html
    [:span#snackbar-mobile-search.Swarmpit-appbar-search-mobile-message
     (comp/input
       {:placeholder      "Search..."
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
        :key        "close"
        :aria-label "Close"
        :color      "inherit"}
       (icon/close
         {:className "Swarmpit-appbar-search-mobile-close"}))]))

(defn- mobile-search [on-change-fn opened]
  (comp/snackbar
    {:open         opened
     :anchorOrigin {:vertical   "top"
                    :horizontal "center"}
     :className    "Swarmpit-appbar-search-mobile"
     :onClose      #(state/update-value [:mobileSearchOpened] false state/layout-cursor)}
    (comp/snackbar-content
      {:aria-describedby "snackbar-mobile-search"
       :className        "Swarmpit-appbar-search-mobile-content"
       :message          (mobile-search-message on-change-fn)
       :action           (mobile-search-action)})))

(defn- appbar-desktop-section
  [items]
  (html
    [:div.Swarmpit-appbar-section-desktop
     items]))

(defn- appbar-mobile-section
  [items]
  (html
    [:div.Swarmpit-appbar-section-mobile
     items]))

(rum/defc appbar < rum/reactive [{:keys [title search-fn actions]}]
  (let [{:keys [mobileSearchOpened menuAnchorEl mobileMoreAnchorEl]} (state/react state/layout-cursor)]
    (comp/mui
      (html
        [:div
         (comp/app-bar
           {:key       "Swarmpit-appbar"
            :color     "primary"
            :className "Swarmpit-appbar"}
           (comp/toolbar
             {:disableGutters false}
             (comp/icon-button
               {:key        "Swarmpit-appbar-menu-btn"
                :color      "inherit"
                :aria-label "Open drawer"
                :onClick    #(state/update-value [:mobileOpened] true state/layout-cursor)
                :className  "Swarmpit-appbar-menu-btn"}
               icon/menu)
             (comp/typography
               {:key       "Swarmpit-appbar-title"
                :className "Swarmpit-appbar-title"
                :variant   "title"
                :color     "inherit"
                :noWrap    true}
               title)
             (html [:div.grow])
             (appbar-desktop-section
               [(when search-fn
                  (search-input search-fn))
                (->> actions (map :button))])
             (appbar-mobile-section
               [(when search-fn
                  (comp/icon-button
                    {:aria-haspopup "true"
                     :onClick       #(state/update-value [:mobileSearchOpened] true state/layout-cursor)
                     :color         "inherit"} icon/search))
                (comp/icon-button
                  {:aria-haspopup "true"
                   :onClick       (fn [e]
                                    (state/update-value [:mobileMoreAnchorEl] (.-currentTarget e) state/layout-cursor))
                   :color         "inherit"} icon/more)])
             (user-menu menuAnchorEl)))
         (mobile-actions-menu actions mobileMoreAnchorEl)
         (mobile-search search-fn mobileSearchOpened)]))))