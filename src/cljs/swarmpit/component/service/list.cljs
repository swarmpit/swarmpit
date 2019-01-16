(ns swarmpit.component.service.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.composite :as composite]
            [material.component.label :as label]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- render-item-ports [item index]
  (html
    (map-indexed
      (fn [i item]
        [:div {:key (str "port-" i "-" index)}
         [:span (:hostPort item)
          [:span.Swarmpit-service-list-port (str " [" (:protocol item) "]")]]]) (:ports item))))

(defn- render-item-replicas [item]
  (let [tasks (get-in item [:status :tasks])]
    (str (:running tasks) " / " (:total tasks))))

(defn- render-item-name [item]
  (html
    [:div
     [:div
      [:span
       (:serviceName item)]]
     [:div
      [:span.Swarmpit-table-cell-secondary
       (get-in item [:repository :image])]]]))

(defn- render-item-update-state [value]
  (case value
    "rollback_started" (label/pulsing "rollback")
    (label/pulsing value)))

(defn- render-item-state [value]
  (case value
    "running" (label/green value)
    "not running" (label/info value)
    "partly running" (label/yellow value)))

(defn- render-status [item]
  (let [update-status (get-in item [:status :update])]
    (if (or (= "updating" update-status)
            (= "rollback_started" update-status))
      (render-item-update-state update-status)
      (render-item-state (:state item)))))

(def render-metadata
  {:table {:summary [{:name      "Service"
                      :render-fn (fn [item] (render-item-name item))}
                     {:name      "Replicas"
                      :render-fn (fn [item] (render-item-replicas item))}
                     {:name      "Ports"
                      :render-fn (fn [item index] (render-item-ports item index))}
                     {:name      ""
                      :render-fn (fn [item] (list/status (render-status item)))}]}
   :list  {:primary   (fn [item] (:serviceName item))
           :secondary (fn [item] (get-in item [:repository :image]))
           :status-fn (fn [item] (render-status item))}})

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :service-info {:id (:serviceName item)})))

(defn- services-handler
  []
  (ajax/get
    (routes/path-for-backend :services)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(def form-actions
  [{:onClick #(dispatch! (routes/path-for-frontend :service-create-image))
    :icon    icon/add-circle
    :name    "New service"}])

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (services-handler))))

(defn linked
  [services]
  (comp/card
    {:className "Swarmpit-card"
     :key       "lsc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "lsch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "services-title"} "Services")})
    (if (empty? services)
      (comp/card-content
        {:key "fccce"}
        (html [:div "No linked services found."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "lscc"}
        (rum/with-key
          (list/responsive
            render-metadata
            services
            onclick-handler) "lsccrl")))))

(defn toolbar-render-metadata
  [filter]
  {:actions [{:name     "New service"
              :onClick  #(dispatch! (routes/path-for-frontend :service-create-image))
              :icon     icon/add-circle-out
              :icon-alt icon/add}]
   :filters [{:checked  (:running filter)
              :name     "Running state"
              :disabled (or (:shutdown filter) false)
              :onClick  #(state/update-value [:filter :running] (not (:running filter)) state/form-state-cursor)}
             {:checked  (:shutdown filter)
              :name     "Shutdown state"
              :disabled (or (:running filter) false)
              :onClick  #(state/update-value [:filter :shutdown] (not (:shutdown filter)) state/form-state-cursor)}]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items (:query filter))
                            (clojure.core/filter #(if (:running filter)
                                                    (= "running" (:state %)) true))
                            (clojure.core/filter #(if (:shutdown filter)
                                                    (= 0 (get-in % [:status :tasks :total])) true)))]
    (progress/form
      loading?
      (common/list "Services"
                   items
                   filtered-items
                   render-metadata
                   onclick-handler
                   (toolbar-render-metadata filter)))))
