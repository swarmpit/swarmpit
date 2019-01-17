(ns swarmpit.component.registry.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.common :as common]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [cljs.core :as core]
            [rum.core :as rum]))

(enable-console-print!)

(def hub-icon (comp/svg {:className "Swarmpit-list-item-icon"} icon/docker-path))
(def reg-icon (comp/svg {:className "Swarmpit-list-item-icon"} icon/registries-path))

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:name item))}
                     {:name      "Type"
                      :render-fn (fn [item] (case (:type item)
                                              "dockeruser" hub-icon
                                              "registry" reg-icon))}
                     {:name      "Url"
                      :render-fn (fn [item] (:url item))}
                     {:name      "Public"
                      :render-fn (fn [item] (if (:public item) "yes" "no"))}
                     ]}
   :list  {:primary   (fn [item] (:name item))
           :secondary (fn [item] (:url item))
           :status-fn (fn [item] (case (:type item)
                                   "dockeruser" hub-icon
                                   "registry" reg-icon))}})

(defn- onclick-handler
  [item]
  (dispatch!
    (routes/path-for-frontend
      (case (:type item)
        "dockeruser" :reg-dockerhub-info
        "registry" :reg-v2-info) {:id (:_id item)})))

(defn- dockerhub-handler
  []
  (ajax/get
    (routes/path-for-backend :dockerhub-users)
    {:state      [:loading? :dockerhub]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items :dockerhub] response state/form-value-cursor))}))

(defn- registries-handler
  []
  (ajax/get
    (routes/path-for-backend :registries)
    {:state      [:loading? :registries]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items :registries] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(defn hub-to-distribution
  [hub-accounts]
  (map #(merge (select-keys % [:_id :type :public :owner])
               {:name (:username %)
                :url  "https://hub.docker.com"}) hub-accounts))

(defn reg-to-distribution
  [reg-accounts]
  (map #(select-keys % [:_id :name :url :type :public :owner]) reg-accounts))

(defn- init-form-state
  []
  (state/set-value {:loading? {:dockerhub  false
                               :registries false}
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (dockerhub-handler)
      (registries-handler))))

(def toolbar-render-metadata
  {:actions [{:name     "Add registry"
              :onClick  #(dispatch! (routes/path-for-frontend :registry-create))
              :icon     icon/add-circle-out
              :icon-alt icon/add}]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        distributions (concat (hub-to-distribution (:dockerhub items))
                              (reg-to-distribution (:registries items)))
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-distributions (-> (core/filter #(= (:owner %) (storage/user)) distributions)
                                   (list-util/filter (:query filter)))]
    (progress/form
      (and (:dockerhub loading?)
           (:registries loading?))
      (common/list "Registries"
                   distributions
                   filtered-distributions
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))

