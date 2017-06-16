(ns swarmpit.component.service.create-image
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.create-image-public :as cip]
            [swarmpit.component.service.create-image-other :as cio]
            [swarmpit.component.service.create-image-user :as ciu]
            [rum.core :as rum]))

(def tabs-inkbar-style
  {:backgroundColor "#437f9d"
   :minWidth        "200px"})

(def tabs-container-style
  {:backgroundColor "#fff"
   :borderBottom    "1px solid rgb(224, 228, 231)"})

(def tab-style
  {:color    "rgb(117, 117, 117"
   :minWidth "200px"})

(rum/defc form < rum/static [registries users]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/services "New service")]]
   [:div.form-panel-tabs
    (comp/mui
      (comp/tabs
        {:key                   "tabs"
         :inkBarStyle           tabs-inkbar-style
         :tabItemContainerStyle tabs-container-style}
        (comp/tab
          {:key       "tab1"
           :label     "SEARCH PUBLIC"
           :className "service-image-tab"
           :icon      (comp/svg icon/search)
           :style     tab-style}
          (cip/form))
        (comp/tab
          {:key       "tab2"
           :label     "SEARCH PRIVATE"
           :className "service-image-tab"
           :icon      (comp/svg icon/docker)
           :style     tab-style
           :onActive  (fn [] (let [state (state/get-value ciu/cursor)
                                   user (:user state)]
                               (if (some? user)
                                 (ciu/repository-handler user))))}
          (ciu/form users))
        (comp/tab
          {:key       "tab3"
           :label     "OTHER REGISTRIES"
           :className "service-image-tab"
           :icon      (comp/svg icon/registries)
           :style     tab-style
           :onActive  (fn [] (let [state (state/get-value cio/cursor)
                                   registry (:registry state)]
                               (if (some? registry)
                                 (cio/repository-handler registry))))}
          (cio/form registries))))]])

(defn- init-public-tab-state
  []
  (state/update-value [:searching] false cip/cursor)
  (state/update-value [:data] [] cip/cursor)
  (state/update-value [:repository] "" cip/cursor))

(defn- init-user-tab-state
  [user]
  (state/update-value [:searching] false ciu/cursor)
  (state/update-value [:data] [] ciu/cursor)
  (state/update-value [:repository] "" ciu/cursor)
  (state/update-value [:user] user ciu/cursor))

(defn- init-other-tab-state
  [registry]
  (state/update-value [:searching] false cio/cursor)
  (state/update-value [:data] [] cio/cursor)
  (state/update-value [:repository] "" cio/cursor)
  (state/update-value [:registry] registry cio/cursor))

(defn- init-state
  [registries users]
  (let [registry (first registries)
        user (first users)]
    (init-public-tab-state)
    (init-user-tab-state user)
    (init-other-tab-state registry)))

(defn mount!
  [registries users]
  (init-state registries users)
  (rum/mount (form registries users) (.getElementById js/document "content")))