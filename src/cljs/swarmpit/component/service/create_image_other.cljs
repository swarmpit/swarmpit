(ns swarmpit.component.service.create-image-other
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
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

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- selected-registry
  [registry-id registries]
  (-> (filter #(= registry-id (:_id %)) registries)
      (first)))

(defn- onclick-handler
  [repository registry]
  (dispatch!
    (routes/path-for-frontend :service-create-config
                              {}
                              {:repository (du/repository (:url registry)
                                                          repository)})))

(defn- repository-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry-repositories {:id registry-id})
    {:state      [:other :searching?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response form-value-cursor))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Repositories fetching failed. " (:error response))))}))

(defn- form-registry-label
  [registry]
  (if (= (storage/user)
         (:owner registry))
    (:name registry)
    (html [:span (:name registry)
           [:span.owner-item (str " [" (:owner registry) "]")]])))

(defn- form-registry [registry registries]
  (comp/text-field
    {:fullWidth       true
     :id              "registry"
     :label           "Registry"
     :select          true
     :value           (:_id registry)
     :margin          "normal"
     :variant         "outlined"
     :InputLabelProps {:shrink true}
     :onChange        (fn [event]
                        (let [value (-> event .-target .-value)]
                          (state/update-value [:registry] (selected-registry value registries) form-state-cursor)
                          (repository-handler value)))}
    (->> registries
         (map #(comp/menu-item
                 {:key   (:_id %)
                  :value (:_id %)} (form-registry-label %))))))

(defn- form-repository [repository]
  (comp/text-field
    {:fullWidth       true
     :id              "repository"
     :label           "Repository"
     :value           repository
     :margin          "normal"
     :variant         "outlined"
     :placeholder     "Find repository"
     :InputLabelProps {:shrink true}
     :onChange        (fn [event]
                        (state/update-value [:repository] (-> event .-target .-value) form-state-cursor))}))

(defn- init-form-state
  [registry]
  (state/set-value {:searching? false
                    :repository ""
                    :registry   registry} form-state-cursor))

(def mixin-init-form
  (mixin/init-tab
    (fn [registries]
      (let [init-registry (first registries)]
        (init-form-state init-registry)
        (when (some? init-registry)
          (repository-handler (:_id init-registry)))))))

(rum/defc form-list < rum/static [searching? registry repositories]
  (html
    [:div
     (when searching?
       (comp/linear-progress))
     (comp/list
       {:dense true}
       (->> repositories
            (map (fn [item]
                   (comp/list-item
                     {:button         true
                      :onClick        #(onclick-handler (:name item) registry)
                      :disableGutters true
                      :divider        true}
                     (comp/list-item-text
                       {:primary   (:name item)
                        :secondary (:description item)}))))))]))

(rum/defc form < rum/reactive
                 mixin-init-form [registries]
  (let [{:keys [repository registry searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)
        filtered-repositories (filter-items repositories repository)]
    (if (some? registry)
      [:div.Swarmpit-image-search
       (form-registry registry registries)
       (form-repository repository)
       (form-list searching? registry filtered-repositories)]
      [:div.Swarmpit-image-search
       (html
         [:span.Swarmpit-message
          (html icon/info)
          (if (storage/admin?)
            [:span "No custom registries found. Add new " [:a {:href (routes/path-for-frontend :registry-create)} "registry."]]
            [:span "No custom registries found. Please ask your admin to setup."])])])))