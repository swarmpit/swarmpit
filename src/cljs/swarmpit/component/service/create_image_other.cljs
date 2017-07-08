(ns swarmpit.component.service.create-image-other
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :service :wizard :image :other])

(def headers [{:name  "Name"
               :width "100%"}])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- repository-handler
  [registry]
  (ajax/GET (routes/path-for-backend :repositories {:registryName registry})
            {:headers {"Authorization" (storage/get "token")}
             :finally (state/update-value [:searching] true cursor)
             :handler (fn [response]
                        (let [res (keywordize-keys response)]
                          (state/update-value [:searching] false cursor)
                          (state/update-value [:data] res cursor)))}))

(defn- form-registry [registry registries]
  (comp/form-comp
    "REGISTRY"
    (comp/select-field
      {:value    registry
       :onChange (fn [_ _ v]
                   (state/update-value [:data] [] cursor)
                   (state/update-value [:registry] v cursor)
                   (repository-handler registry))}
      (->> registries
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- form-repository [repository]
  (comp/form-comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Filter by name"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor))})))

(rum/defc form-loading < rum/static []
  (comp/form-comp-loading true))

(rum/defc form-loaded < rum/static []
  (comp/form-comp-loading false))

(defn- repository-list [registry data]
  (let [repository (fn [index] (:name (nth data index)))]
    (comp/mui
      (comp/table
        {:key         "tbl"
         :selectable  false
         :onCellClick (fn [i]
                        (dispatch!
                          (routes/path-for-frontend :service-create-config
                                                    {}
                                                    {:repository (repository i)
                                                     :registry   registry})))}
        (comp/list-table-header headers)
        (comp/list-table-body headers
                              data
                              render-item
                              [[:name]])))))

(rum/defc form < rum/reactive [registries]
  (let [{:keys [searching
                repository
                registry
                data]} (state/react cursor)
        filtered-data (filter-items data repository)]
    (if (some? registry)
      [:div.form-edit
       (form-registry registry registries)
       (form-repository repository)
       [:div.form-edit-loader
        (if searching
          (form-loading)
          (form-loaded))
        (repository-list registry filtered-data)]]
      [:div.form-edit
       (if (storage/admin?)
         (comp/form-icon-value icon/info [:span "No custom registries found. Add new " [:a {:href (routes/path-for-frontend :registry-create)} "registry."]])
         (comp/form-icon-value icon/info "No custom registries found. Please ask your admin to setup."))])))
