(ns swarmpit.component.container.list
  (:require [material.components :as comp]
            [material.component.label :as label]
            [material.component.list.util :as list-util]
            [swarmpit.component.common :as common]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn owner
  [{:keys [managed project]}]
  (cond managed "Swarm"
        project "Compose"
        :else "Standalone"))

(defn resource-link
  [text route params]
  (comp/link {:href (routes/path-for-frontend route params)
              :onClick #(.stopPropagation %)} text))

(defn container-state
  [{:keys [state]}]
  (label/base state (case state
                     "running" "green"
                     "paused" "yellow"
                     "restarting" "yellow"
                     "dead" "red"
                     "grey")))

(def render-metadata
  {:table {:summary [{:name "Container"
                      :render-fn (fn [item]
                                   (html [:div
                                          [:div (:containerName item)]
                                          [:div.Swarmpit-table-cell-secondary (:image item)]]))}
                     {:name "Node"
                      :render-fn (fn [{:keys [nodeId nodeName]}]
                                   (if nodeId (resource-link nodeName :node-info {:id nodeId}) nodeName))}
                     {:name "Application"
                      :render-fn (fn [{:keys [serviceId serviceName project] :as item}]
                                   (if serviceId
                                     (resource-link (or serviceName serviceId) :service-info {:id serviceId})
                                     (or project (owner item))))}
                     {:name "" :status true
                      :render-fn (fn [item]
                                   (comp/tooltip {:title (or (:status item) "")}
                                     (container-state item)))}]}
   :list {:primary :containerName
          :secondary :image
          :status-fn container-state}})

(defn onclick-handler [item]
  (routes/path-for-frontend :container-info {:id (:id item)}))

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
    (progress/form
      loading?
      (when (some? items)
        (common/list "Containers" items filtered-items render-metadata onclick-handler [])))))
