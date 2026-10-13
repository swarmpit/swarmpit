(ns swarmpit.component.container.list
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.list.util :as list-util]
            [swarmpit.component.common :as common]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(defn owner
  [{:keys [managed project]}]
  (cond managed "Swarm"
        project (str "Compose: " project)
        :else "Standalone"))

(defn container-state
  [{:keys [state]}]
  (label/base state (case state
                     "running" "green"
                     "paused" "yellow"
                     "restarting" "yellow"
                     "dead" "red"
                     "grey")))

(def render-metadata
  {:table {:summary [{:name "Name" :render-fn (fn [item _] (:containerName item))}
                     {:name "Image" :render-fn (fn [item _] (:image item))}
                     {:name "Managed by" :render-fn (fn [item _] (owner item))}
                     {:name "Status" :render-fn (fn [item _] (:status item))}
                     {:name "State" :render-fn (fn [item _] (container-state item))}]}
   :list {:primary :containerName
          :secondary (fn [item] (str (:image item) " · " (owner item)))
          :status-fn container-state}})

(defn- containers-handler
  []
  (when-not (or (state/get-value (conj state/form-state-cursor :loading?))
                (state/get-value (conj state/form-state-cursor :refreshing?)))
    (ajax/get
      (routes/path-for-backend :containers)
      {:state (if (some? (state/get-value (conj state/form-value-cursor :items)))
                [:refreshing?] [:loading?])
       :on-success (fn [{:keys [response origin?]}]
                     (when origin?
                       (state/update-value [:items] response state/form-value-cursor)))})))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (state/set-value {:loading? false} state/form-state-cursor)
      (containers-handler))))

(def mixin-refresh-form
  (mixin/refresh-form (fn [_] (containers-handler)) 5000))

(rum/defc form < rum/reactive mixin-init-form mixin-refresh-form [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)
        {:keys [query]} (state/react state/search-cursor)
        filtered-items (sort-by :containerName (list-util/filter items query))]
    [:div
     (comp/box {:style {:padding "16px 24px 0"}}
       (form/item-info "Containers on the Docker host connected to Swarmpit, including stopped containers. Swarm containers are managed through their services."))
     (progress/form
       loading?
       (when (some? items)
         (common/list "Containers" items filtered-items render-metadata
                      #(routes/path-for-frontend :container-info {:id (:id %)})
                      [{:name "Refresh" :onClick containers-handler :variant "outlined"}])))]))
