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

(def drawer-width 200)

(def styles
  (let [theme (js->clj comp/theme)
        transitions (-> theme (get "transitions"))
        transitions-create (get transitions "create")
        transition-easing (-> transitions (get-in ["easing" "sharp"]))
        transition-duration-leaving (-> transitions (get-in ["duration" "leavingScreen"]))
        transition-duration-entering (-> transitions (get-in ["duration" "enteringScreen"]))]
    {:appBar      {:zIndex     (+ (get-in theme ["zIndex" "drawer"]) 1)
                   :transition (transitions-create
                                 (clj->js ["width" "margin"])
                                 (clj->js {:easing   transition-easing
                                           :duration transition-duration-leaving}))}
     :appBarShift {:marginLeft drawer-width
                   :width      (str "calc(100% - " drawer-width "px)")
                   :transition (transitions-create
                                 (clj->js ["width" "margin"])
                                 (clj->js {:easing   transition-easing
                                           :duration transition-duration-entering}))}
     :menuButton  {:marginLeft  12
                   :marginRight 36}
     :hide        {:display "none"}}))

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
                         (state/update-value [:anchorEl] (.-currentTarget e) state/layout-cursor))
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
        :onClose         #(state/update-value [:anchorEl] nil state/layout-cursor)}
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

(defn- search-input [placeholder on-change-fn]
  (html
    [:div.Swarmpit-appbar-search
     [:div.Swarmpit-appbar-search-icon icon/search]
     (comp/input
       {:placeholder      placeholder
        :onChange         on-change-fn
        :classes          {:root  "Swarmpit-appbar-search-root"
                           :input "Swarmpit-appbar-search-input"}
        :id               "Swarmpit-appbar-search-filter"
        :disableUnderline true})]))

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

(rum/defc appbar < rum/reactive [{:keys [title search actions]}]
  (let [{:keys [opened anchorEl mobileMoreAnchorEl]} (state/react state/layout-cursor)]
    (comp/mui
      (html
        [:div
         (comp/app-bar
           {:position  "absolute"
            :key       "Swarmpit-appbar"
            :color     "primary"
            :className (if opened
                         "Swarmpit-appbar Swarmpit-appbar-shift"
                         "Swarmpit-appbar")}
           (comp/toolbar
             {:disableGutters (not opened)}
             (comp/icon-button
               {:key        "Swarmpit-appbar-menu-btn"
                :color      "inherit"
                :aria-label "Open drawer"
                :onClick    #(state/update-value [:opened] true state/layout-cursor)
                :className  (if opened
                              "Swarmpit-appbar-menu-btn hide"
                              "Swarmpit-appbar-menu-btn")}
               icon/menu)
             (comp/typography
               {:key       "Swarmpit-appbar-title"
                :className "Swarmpit-appbar-title"
                :variant   "title"
                :color     "inherit"
                :noWrap    true}
               title)
             (when search
               (search-input (:placeholder search)
                             (:on-change search)))
             (html
               [:div.grow])
             (html
               [:div.Swarmpit-appbar-section-desktop
                (->> actions
                     (map :button))])
             (html
               [:div.Swarmpit-appbar-section-mobile
                (comp/icon-button
                  {:aria-haspopup "true"
                   :onClick       (fn [e]
                                    (state/update-value [:mobileMoreAnchorEl] (.-currentTarget e) state/layout-cursor))
                   :color         "inherit"} icon/more)])
             (html
               [:div
                (comp/icon-button
                  {:aria-owns     (when anchorEl "Swarmpit-appbar-user-menu")
                   :aria-haspopup "true"
                   :onClick       (fn [e]
                                    (state/update-value [:anchorEl] (.-currentTarget e) state/layout-cursor))
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
                   :onClose         #(state/update-value [:anchorEl] nil state/layout-cursor)}
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
                                  (routes/path-for-frontend :login)))} "Log out"))])))
         (mobile-actions-menu actions mobileMoreAnchorEl)]))))
