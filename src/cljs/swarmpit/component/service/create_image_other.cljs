(ns swarmpit.component.service.create-image-other
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.handler :as handler]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def cursor [:form :other])

(def headers [{:name  "Name"
               :width "100%"}])

(defonce searching? (atom false))

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
    {:state      searching?
     :on-success (fn [response]
                   (state/update-value [:repositories] response cursor))}))

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

(defn- init-state
  [registry]
  (state/set-value {:repositories []
                    :repository   ""
                    :registry     registry} cursor))

(def mixin-init-form
  (mixin/init-form-tab
    (fn [registries]
      (init-state (first registries)))))

(rum/defc form-list < rum/static [searching? registry repositories]
  (let [repository (fn [index] (:name (nth repositories index)))]
    [:div.form-edit-loader
     (if searching?
       (progress/loading)
       (progress/loaded))
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
                          repositories
                          render-item
                          [[:name]])))]))

(rum/defc form < rum/reactive
                 mixin-init-form [registries]
  (let [{:keys [repository registry repositories]} (state/react cursor)
        filtered-repositories (filter-items repositories repository)]
    (if (some? registry)
      [:div.form-edit
       (form-registry registry registries)
       (form-repository repository)
       (form-list (rum/react searching?) registry filtered-repositories)]
      [:div.form-edit
       (if (storage/admin?)
         (form/icon-value icon/info [:span "No custom registries found. Add new " [:a {:href (routes/path-for-frontend :registry-create)} "registry."]])
         (form/icon-value icon/info "No custom registries found. Please ask your admin to setup."))])))