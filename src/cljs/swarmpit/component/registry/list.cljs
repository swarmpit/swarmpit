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
(def amazon-icon (comp/svg {:className "Swarmpit-list-item-icon"} icon/amazon-path))
(def azure-icon (comp/svg {:className "Swarmpit-list-item-icon"} icon/azure-path))
(def google-icon (comp/svg {:className "Swarmpit-list-item-icon"} icon/google-path))

(defn icon-type [item]
  (comp/tooltip
    {:title     (case (:type item)
                  "dockeruser" "dockerhub"
                  "registry" "registry v2"
                  "ecr" "amazon ecr")
     :placement "left"}
    (case (:type item)
      "dockeruser" hub-icon
      "registry" reg-icon
      "ecr" amazon-icon)))

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:name item))}
                     {:name      "Type"
                      :render-fn (fn [item] (icon-type item))}
                     {:name      "Url"
                      :render-fn (fn [item] (:url item))}
                     {:name      "Public"
                      :render-fn (fn [item] (if (:public item) "yes" "no"))}]}
   :list  {:primary   (fn [item] (:name item))
           :secondary (fn [item] (:url item))
           :status-fn (fn [item] (icon-type item))}})

(defn onclick-handler
  [item]
  (routes/path-for-frontend
    (case (:type item)
      "dockeruser" :reg-dockerhub-info
      "registry" :reg-v2-info
      "ecr" :reg-ecr-info) {:id (:_id item)}))

(defn- dockerhub-handler
  []
  (ajax/get
    (routes/path-for-backend :dockerhub-users)
    {:state      [:loading? :dockerhub]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :dockerhub] response state/form-value-cursor)))}))

(defn- registries-handler
  []
  (ajax/get
    (routes/path-for-backend :registries)
    {:state      [:loading? :registries]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :registries] response state/form-value-cursor)))}))

(defn- ecrs-handler
  []
  (ajax/get
    (routes/path-for-backend :ecrs)
    {:state      [:loading? :ecrs]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :ecrs] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn hub-to-distribution
  [hub-accounts]
  (map #(merge (select-keys % [:_id :type :public :owner])
               {:name (:username %)
                :url  "https://hub.docker.com"}) hub-accounts))

(defn reg-to-distribution
  [reg-accounts]
  (map #(select-keys % [:_id :name :url :type :public :owner]) reg-accounts))

(defn ecr-to-distribution
  [ecr-accounts]
  (map #(merge (select-keys % [:_id :url :type :public :owner])
               {:name (:user %)}) ecr-accounts))

(defn- init-form-state
  []
  (state/set-value {:loading? {:dockerhub  false
                               :registries false
                               :ecrs       false}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (dockerhub-handler)
      (registries-handler)
      (ecrs-handler))))

(def toolbar-render-metadata
  {:actions [{:name     "Link registry"
              :onClick  #(dispatch! (routes/path-for-frontend :registry-create))
              :icon     icon/add-circle-out
              :icon-alt icon/add}]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)
        {:keys [query]} (state/react state/search-cursor)
        distributions (concat (hub-to-distribution (:dockerhub items))
                              (reg-to-distribution (:registries items))
                              (ecr-to-distribution (:ecrs items)))
        filtered-distributions (-> (core/filter #(= (:owner %) (storage/user)) distributions)
                                   (list-util/filter query))]
    (progress/form
      (and (:dockerhub loading?)
           (:registries loading?)
           (:ecrs loading?))
      (common/list "Registries"
                   distributions
                   filtered-distributions
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))

