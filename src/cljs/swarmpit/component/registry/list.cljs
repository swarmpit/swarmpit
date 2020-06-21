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
(def gitlab-icon (comp/svg {:className "Swarmpit-list-item-icon"} icon/gitlab-path))

(defn icon-type [item]
  (comp/tooltip
    {:title     (case (:type item)
                  "dockerhub" "dockerhub"
                  "v2" "registry v2"
                  "ecr" "amazon ecr"
                  "acr" "azure acr"
                  "gitlab" "gitlab registry")
     :placement "left"}
    (case (:type item)
      "dockerhub" hub-icon
      "v2" reg-icon
      "ecr" amazon-icon
      "acr" azure-icon
      "gitlab" gitlab-icon)))

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
  (routes/path-for-frontend :registry-info {:id           (:_id item)
                                            :registryType (:type item)}))

(defn- registries-dockerhub-handler
  []
  (ajax/get
    (routes/path-for-backend :registries {:registryType :dockerhub})
    {:state      [:loading? :dockerhub]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :dockerhub] response state/form-value-cursor)))}))

(defn- registries-v2-handler
  []
  (ajax/get
    (routes/path-for-backend :registries {:registryType :v2})
    {:state      [:loading? :v2]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :v2] response state/form-value-cursor)))}))

(defn- registries-ecr-handler
  []
  (ajax/get
    (routes/path-for-backend :registries {:registryType :ecr})
    {:state      [:loading? :ecr]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :ecr] response state/form-value-cursor)))}))

(defn- registries-acr-handler
  []
  (ajax/get
    (routes/path-for-backend :registries {:registryType :acr})
    {:state      [:loading? :acr]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :acr] response state/form-value-cursor)))}))

(defn- registries-gitlab-handler
  []
  (ajax/get
    (routes/path-for-backend :registries {:registryType :gitlab})
    {:state      [:loading? :gitlab]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items :gitlab] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn hub-to-distribution
  [reg-hub-accounts]
  (map #(merge (select-keys % [:_id :type :public :owner])
               {:name (:username %)
                :url  "https://hub.docker.com"}) reg-hub-accounts))

(defn v2-to-distribution
  [reg-v2-accounts]
  (map #(select-keys % [:_id :name :url :type :public :owner]) reg-v2-accounts))

(defn ecr-to-distribution
  [reg-ecr-accounts]
  (map #(merge (select-keys % [:_id :url :type :public :owner])
               {:name (:user %)}) reg-ecr-accounts))

(defn acr-to-distribution
  [reg-acr-accounts]
  (map #(merge (select-keys % [:_id :url :type :public :owner])
               {:name (:spName %)}) reg-acr-accounts))

(defn gitlab-to-distribution
  [reg-gitlab-accounts]
  (map #(merge (select-keys % [:_id :url :type :public :owner])
               {:name (:username %)}) reg-gitlab-accounts))

(defn- init-form-state
  []
  (state/set-value {:loading? {:dockerhub false
                               :v2        false
                               :ecr       false
                               :acr       false
                               :gitlab    false}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (registries-dockerhub-handler)
      (registries-v2-handler)
      (registries-ecr-handler)
      (registries-acr-handler)
      (registries-gitlab-handler))))

(def toolbar-render-metadata
  [{:name     "Link registry"
    :onClick  #(dispatch! (routes/path-for-frontend :registry-create))
    :primary  true
    :icon     (icon/add-circle-out)
    :icon-alt (icon/add)}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)
        {:keys [query]} (state/react state/search-cursor)
        distributions (concat (hub-to-distribution (:dockerhub items))
                              (v2-to-distribution (:v2 items))
                              (ecr-to-distribution (:ecr items))
                              (acr-to-distribution (:acr items))
                              (gitlab-to-distribution (:gitlab items)))
        filtered-distributions (-> (core/filter #(= (:owner %) (storage/user)) distributions)
                                   (list-util/filter query))]
    (progress/form
      (and (:dockerhub loading?)
           (:v2 loading?)
           (:ecr loading?)
           (:acr loading?)
           (:gitlab loading?))
      (common/list "Registries"
                   distributions
                   filtered-distributions
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))

