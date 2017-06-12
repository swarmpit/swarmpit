(ns swarmpit.component.service.create-image
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.create-image-dockerhub :as cid]
            [swarmpit.component.service.create-image-other :as cio]
            [swarmpit.component.service.create-image-repository :as cir]
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
        {:inkBarStyle           tabs-inkbar-style
         :tabItemContainerStyle tabs-container-style}
        (comp/tab
          {:key   "tab1"
           :label "SEARCH DOCKERHUB"
           :style tab-style}
          (cid/form))
        (comp/tab
          {:key      "tab2"
           :label    "DOCKERHUB USERS"
           :style    tab-style
           :onActive (fn [] (let [state (state/get-value cir/cursor)
                                  user (:user state)]
                              (if (some? user)
                                (cir/repository-handler user 1))))}
          (cir/form users))
        (comp/tab
          {:key   "tab3"
           :label "OTHER REGISTRIES"
           :style tab-style}
          (cio/form registries))))]])

(defn- init-dockerhub-tab-state
  []
  (state/update-value [:searching] false cid/cursor)
  (state/update-value [:data] [] cid/cursor)
  (state/update-value [:repository] "" cid/cursor))

(defn- init-repository-tab-state
  [user]
  (state/update-value [:loading] false cir/cursor)
  (state/update-value [:data] [] cir/cursor)
  (state/update-value [:user] user cir/cursor))

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
    (init-dockerhub-tab-state)
    (init-repository-tab-state user)
    (init-other-tab-state registry)))

(defn mount!
  [registries users]
  (init-state registries users)
  (rum/mount (form registries users) (.getElementById js/document "content")))