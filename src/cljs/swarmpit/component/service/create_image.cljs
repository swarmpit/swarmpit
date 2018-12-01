(ns swarmpit.component.service.create-image
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.create-image-public :as cip]
            [swarmpit.component.service.create-image-other :as cio]
            [swarmpit.component.service.create-image-private :as ciu]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- registries-handler
  []
  (ajax/get
    (routes/path-for-backend :registries)
    {:state      [:registries :loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:registries :list] response state/form-state-cursor))}))

(defn- users-handler
  []
  (ajax/get
    (routes/path-for-backend :dockerhub-users)
    {:state      [:users :loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:users :list] response state/form-state-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:registries {:loading? true
                                 :list     []}
                    :users      {:loading? true
                                 :list     []}
                    :tab        0} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:public  {}
                    :private {}
                    :other   {}} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value)
      (registries-handler)
      (users-handler))))

(rum/defc form-tabs < rum/static [registries users tab]
  (html
    [:div.Swarmpit-form
     [:div.Swarmpit-form-context
      (comp/mui
        (comp/card
          {:className "Swarmpit-form-card"}
          (comp/card-header
            {:className "Swarmpit-form-card-header"
             :title     "Image Registry"
             :subheader "Search for images accross public and private registries"})
          (comp/card-content
            {:className "Swarmpit-table-card-content"}
            (comp/tabs
              {:key            "tabs"
               :style          {:backgroundColor "#f5f5f5"
                                :borderTop       "1px solid rgba(0, 0, 0, 0.12)"
                                :borderBottom    "1px solid rgba(0, 0, 0, 0.12)"}
               :value          tab
               :onChange       (fn [e v]
                                 (state/update-value [:tab] v state/form-state-cursor))
               :fullWidth      true
               :indicatorColor "primary"
               :textColor      "primary"
               :centered       true}
              (comp/tab
                {:key   "tab1"
                 :label "SEARCH PUBLIC"
                 :icon  icon/search})
              (comp/tab
                {:key   "tab2"
                 :label "SEARCH PRIVATE"
                 :icon  (comp/svg icon/docker)})
              (comp/tab
                {:key   "tab3"
                 :label "OTHER REGISTRIES"
                 :icon  (comp/svg icon/registries)})))
          (comp/card-content
            {}
            (case tab
              0 (cip/form)
              1 (ciu/form users)
              2 (cio/form registries)))
          (comp/card-content
            {:className "Swarmpit-table-card-content"}
            (case tab
              0 (cip/form-list)
              1 (ciu/form-list)
              2 (cio/form-list)))))]]))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [registries users tab]} (state/react state/form-state-cursor)]
    (progress/form
      (or (:loading? registries)
          (:loading? users))
      (form-tabs (:list registries)
                 (:list users)
                 tab))))