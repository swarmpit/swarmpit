(ns swarmpit.component.service.create-image
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :service :wizard :image])

(defn- dockerhub?
  [registry]
  (= registry "dockerhub"))

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-dockerhub-handler
  [name query page]
  (ajax/GET (routes/path-for-backend :repositories {:registryName name})
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repositoryQuery query
                       :repositoryPage  page}
             :finally (state/update-value [:searching] true cursor)
             :handler (fn [response]
                        (let [res (keywordize-keys response)]
                          (state/update-value [:searching] false cursor)
                          (state/update-value [:data] res cursor)))}))

(defn- repository-handler
  [name query]
  (ajax/GET (routes/path-for-backend :repositories {:registryName name})
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repositoryQuery query}
             :finally (state/update-value [:searching] true cursor)
             :handler (fn [response]
                        (let [res (keywordize-keys response)]
                          (state/update-value [:searching] false cursor)
                          (state/update-value [:data] res cursor)))}))

(defn- form-registry [selected registries]
  (comp/form-comp
    "REGISTRY"
    (comp/select-field
      {:value    selected
       :onChange (fn [_ _ v]
                   (state/update-value [:data] [] cursor)
                   (state/update-value [:registry] v cursor))}
      (->> registries
           (map #(comp/menu-item
                   {:key         (:name %)
                    :value       (:name %)
                    :primaryText (:name %)}))))))

(defn- form-repository [repository registry]
  (comp/form-comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Find repository"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor)
                   (if (dockerhub? registry)
                     (repository-dockerhub-handler registry v 1)
                     (repository-handler registry v)))})))

(defn- repository-dockerhub-list [data registry]
  (let [{:keys [results page limit total query]} data
        offset (* limit (- page 1))
        repository (fn [index] (:repo_name (nth results index)))]
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
        (comp/list-table-header ["Name" "Description"])
        (comp/list-table-body results
                              render-item
                              [[:repo_name] [:short_description]])
        (if (not (empty? results))
          (comp/list-table-paging offset
                                  total
                                  limit
                                  #(repository-dockerhub-handler registry query (- (js/parseInt page) 1))
                                  #(repository-dockerhub-handler registry query (+ (js/parseInt page) 1))))))))

(defn- repository-list [data registry]
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
        (comp/list-table-header ["Name"])
        (comp/list-table-body data
                              render-item
                              [[:name]])))))

(rum/defc form-loading < rum/static []
  (comp/form-comp-loading true))

(rum/defc form-loaded < rum/static []
  (comp/form-comp-loading false))

(rum/defc form < rum/reactive [registries]
  (let [{:keys [searching repository registry data]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services "New service")]]

     ;[:div {:style {:display        "flex"
     ;               :justifyContent "center"
     ;               :borderBottom   "1px solid rgb(224, 224, 224)"}}
     ; (comp/mui
     ;   (comp/icon-button
     ;     {:tooltip "Search in registries"}
     ;     (comp/svg
     ;       {:key "user-menu-button-icon"}
     ;       icon/registries)))
     ; (comp/mui
     ;   (comp/icon-button
     ;     {:tooltip "Show user repositories"}
     ;     (comp/svg
     ;       {:key "user-menu-button-icon"}
     ;       icon/images)))]

     [:div.form-edit
      (form-registry registry registries)
      (form-repository repository registry)
      [:div.form-edit-loader
       (if searching
         (form-loading)
         (form-loaded))
       (if (dockerhub? registry)
         (repository-dockerhub-list data registry)
         (repository-list data registry))]]]))

(defn- init-state
  [registries]
  (let [registry (first registries)]
    (state/update-value [:searching] false cursor)
    (state/update-value [:data] [] cursor)
    (state/update-value [:repository] "" cursor)
    (state/update-value [:registry] (:name registry) cursor)))

(defn mount!
  [registries]
  (init-state registries)
  (rum/mount (form registries) (.getElementById js/document "content")))