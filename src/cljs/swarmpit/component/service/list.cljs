(ns swarmpit.component.service.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- render-item-replicas-pie [item]
  (let [tasks (get-in item [:status :tasks])
        data (->> (range 0 (:total tasks))
                  (map (fn [num]
                         (if (< num (:running tasks))
                           {:name  (str "Replica " (inc num))
                            :value 1
                            :color "#43a047"}
                           {:name  (str "Replica " (inc num))
                            :value 1
                            :color "#6c757d"})))
                  (into []))]
    (print data)
    (comp/pie-chart
      {:width  150
       :height 150}
      (comp/pie
        {:data        data
         :cx          "50"
         :cy          "50"
         :innerRadius "60%"
         :outerRadius "80%"
         :startAngle  90
         :endAngle    -270
         :fill        "#8884d8"}
        (map #(comp/cell {:fill (:color %)}) data)
        (comp/re-label
          {:width    30
           :position "center"} (str (:total tasks) " replicas"))))))

(defn- render-item-ports [item]
  (html
    (for [port (:ports item)]
      [:div
       [:span (:hostPort port)
        [:span.Swarmpit-service-list-port (str " [" (:protocol port) "]")]]])))

(defn- render-item-replicas [item]
  (let [tasks (get-in item [:status :tasks])]
    (str (:running tasks) " / " (:total tasks))))

(defn- render-item-name [item]
  (html
    [:div
     [:div
      [:span [:b (:serviceName item)]]]
     [:div
      [:span (get-in item [:repository :image])]]]))

(defn- render-item-update-state [value]
  (case value
    "rollback_started" (label/info "rollback")
    (label/info value)))

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

(defn- render-state-fn
  [item]
  (case (:state item)
    "running" [:div.Swarmpit-icon-ok icon/check-circle]
    "not running" [:div.Swarmpit-icon-info icon/cancel]
    "partly running" [:div.Swarmpit-icon-warning icon/check-circle]))

(def render-metadata
  {:table {:title     "Overview"
           :subheader "RUNNING: 5, UPDATING: 0"
           :summary   [{:name      "Service"
                        :render-fn (fn [item] (render-item-name item))}
                       {:name      "Replicas"
                        :render-fn (fn [item] (render-item-replicas item))}
                       {:name      "Ports"
                        :render-fn (fn [item] (render-item-ports item))}
                       {:name      "Status"
                        :render-fn (fn [item] (render-status item))}]}
   :list  {:title         "Overview"
           :subheader     "RUNNING: 5, UPDATING: 0"
           :primary-key   (fn [item] (:serviceName item))
           :secondary-key (fn [item] (get-in item [:repository :image]))
           :status-fn     (fn [item] (label/green "running"))
           :summary       [{:render-fn (fn [item] (render-item-replicas-pie item))}]}})

(defn- onclick-handler
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
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch! (routes/path-for-frontend :service-create-image))} icon/add-circle)
    :name   "New service"}])

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (services-handler))))

;(defn linked-services
;  [services]
;  (when (not-empty services)
;    [:div.form-layout-group.form-layout-group-border
;     (form/section "Linked Services")
;     (list-auto/table (map :name headers)
;                      services
;                      render-item
;                      render-item-keys
;                      onclick-handler)]))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (list-util/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (list/responsive
            render-metadata
            (sort-by :serviceName filtered-items)
            onclick-handler)]]))))