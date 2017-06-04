(ns swarmpit.component.repository.v2.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [cemerick.url :refer [map->query]]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [clojure.walk :as walk]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :repository :list :data])

(def headers ["Name"])

(def render-item-keys
  [[:name]])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-handler
  [name query]
  (ajax/GET (str "v2/registries/" name "/repo")
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repositoryQuery query}
             :handler (fn [response]
                        (let [res (walk/keywordize-keys response)]
                          (state/set-value res cursor)))}))

(defn- form-repository [registry-name]
  (comp/form-comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Find repository"
       :onChange (fn [_ v]
                   (repository-handler registry-name v))})))

(rum/defc repository-list < rum/reactive [registry-name]
  (let [items (state/react cursor)
        repository (fn [index] (:name (nth items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/create "Step 2")]]
     (form-repository registry-name)
     (comp/mui
       (comp/table
         {:key         "tbl"
          :selectable  false
          :onCellClick (fn [i]
                         (dispatch! (str "/#/services/create/wizard/config?"
                                         (map->query {:repository      (repository i)
                                                      :registry        registry-name
                                                      :registryVersion "v2"}))))}
         (comp/list-table-header headers)
         (comp/list-table-body items
                               render-item
                               render-item-keys)))]))

(defn- init-state
  []
  (state/set-value {} cursor))

(defn mount!
  [registry-name]
  (rum/mount (repository-list registry-name) (.getElementById js/document "content")))