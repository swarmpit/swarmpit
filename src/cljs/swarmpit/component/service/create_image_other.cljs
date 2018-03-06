(ns swarmpit.component.service.create-image-other
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.message :as message]
            [swarmpit.docker.utils :as du]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def form-value-cursor (conj state/form-value-cursor :other))

(def form-state-cursor (conj state/form-state-cursor :other))

(def headers [{:name  "Name"
               :width "100%"}])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- selected-registry
  [registry-id registries]
  (-> (filter #(= registry-id (:_id %)) registries)
      (first)))

(defn- onclick-handler
  [index registry repositories]
  (let [repository (fn [index] (:name (nth repositories index)))]
    (dispatch!
      (routes/path-for-frontend :service-create-config
                                {}
                                {:repository (du/repository (:url registry)
                                                            (repository index))}))))

(defn- repository-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry-repositories {:id registry-id})
    {:state      [:other :searching?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response form-value-cursor))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Repositories fetching failed. Reason: " (:error response))))}))

(defn- form-registry-label
  [registry]
  (if (= (storage/user)
         (:owner registry))
    (:name registry)
    (html [:span (:name registry)
           [:span.owner-item (str " [" (:owner registry) "]")]])))

(defn- form-registry [registry registries]
  (form/comp
    "REGISTRY"
    (comp/select-field
      {:value    (:_id registry)
       :onChange (fn [_ _ v]
                   (state/update-value [:registry] (selected-registry v registries) form-state-cursor)
                   (repository-handler v))}
      (->> registries
           (map #(comp/menu-item
                   {:key         (:_id %)
                    :value       (:_id %)
                    :primaryText (form-registry-label %)}))))))

(defn- form-repository [repository]
  (form/comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Filter by name"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v form-state-cursor))})))

(defn- init-form-state
  [registry]
  (state/set-value {:searching? false
                    :repository ""
                    :registry   registry} form-state-cursor))

(def mixin-init-form
  (mixin/init-tab
    (fn [registries]
      (init-form-state (first registries)))))

(rum/defc form-list < rum/static [searching? registry repositories]
  [:div.form-edit-loader
   (if searching?
     (progress/loading)
     (progress/loaded))
   (comp/mui
     (comp/table
       {:key         "tbl"
        :selectable  false
        :onCellClick (fn [i] (onclick-handler i registry repositories))}
       (list/table-header headers)
       (list/table-body headers
                        repositories
                        render-item
                        [[:name]])))])

(rum/defc form < rum/reactive
                 mixin-init-form [registries]
  (let [{:keys [repository registry searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)
        filtered-repositories (filter-items repositories repository)]
    (if (some? registry)
      [:div.form-edit
       (form-registry registry registries)
       (form-repository repository)
       (form-list searching? registry filtered-repositories)]
      [:div.form-edit
       (if (storage/admin?)
         (form/icon-value icon/info [:span "No custom registries found. Add new " [:a {:href (routes/path-for-frontend :registry-create)} "registry."]])
         (form/icon-value icon/info "No custom registries found. Please ask your admin to setup."))])))