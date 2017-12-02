(ns swarmpit.component.service.create-image-other
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

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
  [registry-id]
  (handler/get
    (routes/path-for-backend :registry-repositories {:id registry-id})
    {:on-call    (state/update-value [:searching] true cursor)
     :on-success (fn [response]
                   (state/update-value [:searching] false cursor)
                   (state/update-value [:data] response cursor))
     :on-error   (fn [_]
                   (state/update-value [:searching] false cursor))}))

(defn- form-registry-label
  [registry]
  (if (= (storage/user)
         (:owner registry))
    (:name registry)
    (html [:span (:name registry)
           [:span.owner-item (str " [" (:owner registry) "]")]])))

(defn- form-registry [registry registries]
  (let [registry-by-id (fn [id] (first (filter #(= id (:_id %)) registries)))]
    (form/comp
      "REGISTRY"
      (comp/select-field
        {:value    (:_id registry)
         :onChange (fn [_ _ v]
                     (state/update-value [:data] [] cursor)
                     (state/update-value [:registry] (registry-by-id v) cursor)
                     (repository-handler v))}
        (->> registries
             (map #(comp/menu-item
                     {:key         (:_id %)
                      :value       (:_id %)
                      :primaryText (form-registry-label %)})))))))

(defn- form-repository [repository]
  (form/comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Filter by name"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor))})))

(rum/defc form-loading < rum/static []
  (form/loading true))

(rum/defc form-loaded < rum/static []
  (form/loading false))

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
                                                    {:repository       (repository i)
                                                     :distributionType "registry"
                                                     :distribution     (:_id registry)})))}
        (list/table-header headers)
        (list/table-body headers
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
         (form/icon-value icon/info [:span "No custom registries found. Add new " [:a {:href (routes/path-for-frontend :registry-create)} "registry."]])
         (form/icon-value icon/info "No custom registries found. Please ask your admin to setup."))])))