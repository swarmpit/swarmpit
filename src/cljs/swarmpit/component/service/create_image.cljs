(ns swarmpit.component.service.create-image
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.create-image-public :as cip]
            [swarmpit.component.service.create-image-other :as cio]
            [swarmpit.component.service.create-image-private :as ciu]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(def tabs-inkbar-style
  {:backgroundColor "#437f9d"
   :minWidth        "200px"})

(def tabs-container-style
  {:backgroundColor "#fff"
   :borderBottom    "1px solid rgb(224, 228, 231)"})

(def tab-style
  {:color    "rgb(117, 117, 117"
   :minWidth "200px"})

(defonce registries (atom []))

(defonce registries-loading? (atom false))

(defonce users (atom []))

(defonce users-loading? (atom false))

(defn- registries-handler
  []
  (ajax/get
    (routes/path-for-backend :registries)
    {:state      registries-loading?
     :on-success (fn [response]
                   (reset! registries response))}))

(defn- users-handler
  []
  (ajax/get
    (routes/path-for-backend :dockerhub-users)
    {:state      users-loading?
     :on-success (fn [response]
                   (reset! users response))}))

(defn- init-state
  []
  (state/set-value {:public  {}
                    :private {}
                    :other   {}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (registries-handler)
      (users-handler))))

(rum/defc form-tabs < rum/static [registries users]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/services "New service")]]
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
           :onActive  (fn []
                        (let [state (state/get-value ciu/cursor)
                              user (:user state)]
                          (when (some? user)
                            (ciu/repository-handler (:_id user)))))}
          (ciu/form users))
        (comp/tab
          {:key       "tab3"
           :label     "OTHER REGISTRIES"
           :className "service-image-tab"
           :icon      (comp/svg icon/registries)
           :style     tab-style
           :onActive  (fn []
                        (let [state (state/get-value cio/cursor)
                              registry (:registry state)]
                          (when (some? registry)
                            (cio/repository-handler (:_id registry)))))}
          (cio/form registries))))]])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [registries (rum/react registries)
        users (rum/react users)]
    (progress/form
      (or (rum/react registries-loading?)
          (rum/react users-loading?))
      (form-tabs registries users))))