(ns swarmpit.component.service.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
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

(defn- render-item-update-state [value]
  (case value
    "rollback_started" (label/base "rollback" "pulsing")
    (label/base value "pulsing")))

(defn- render-item-state [value]
  (case value
    "running" (label/base value "green")
    "not running" (label/base value "info")
    "partly running" (label/base value "yellow")))

(defn- render-status [item]
  (let [update-status (get-in item [:status :update])]
    (html
      [:span.Swarmpit-table-status
       (if (or (= "updating" update-status)
               (= "rollback_started" update-status))
         (render-item-update-state update-status)
         (render-item-state (:state item)))])))

(def render-metadata
  {:table {:summary [{:name      "Service"
                      :render-fn (fn [item] (list/table-item-name (:serviceName item) (get-in item [:repository :image])))}
                     {:name      "Replicas"
                      :render-fn (fn [item] (render-item-replicas item))}
                     {:name      "Ports"
                      :render-fn (fn [item index] (render-item-ports item index))}
                     {:name      ""
                      :status    true
                      :render-fn (fn [item] (render-status item))}]}
   :list  {:primary   (fn [item] (:serviceName item))
           :secondary (fn [item] (get-in item [:repository :image]))
           :status-fn (fn [item] (render-status item))}})

(defn onclick-handler
  [item]
  (routes/path-for-frontend :service-info {:id (:serviceName item)}))

(defn- services-handler
  []
  (ajax/get
    (routes/path-for-backend :services)
    {:state      [:loading?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading?    false
                    :filter      {:state nil}
                    :filterOpen? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (services-handler))))

(defn linked
  [services]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Services")})
    (if (empty? services)
      (comp/card-content
        {}
        (form/item-info "No linked services found."))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive
          render-metadata
          services
          onclick-handler)))))

(defn pinned
  [services]
  (comp/card
    {:className "Swarmpit-card"}
    (when services
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive
          render-metadata
          services
          onclick-handler)))))

(rum/defc form-filters < rum/static [filterOpen? {:keys [state] :as filter}]
  (common/list-filters
    filterOpen?
    (comp/text-field
      {:fullWidth       true
       :label           "State"
       :helperText      "Filter by service state"
       :select          true
       :value           state
       :variant         "outlined"
       :margin          "normal"
       :InputLabelProps {:shrink true}
       :onChange        #(state/update-value [:filter :state] (-> % .-target .-value) state/form-state-cursor)}
      (comp/menu-item
        {:key   "running"
         :value "running"} "running")
      (comp/menu-item
        {:key   "shutdown"
         :value "shutdown"} "shutdown"))))

(def toolbar-render-metadata
  [{:name     "New service"
    :onClick  #(dispatch! (routes/path-for-frontend :service-create-image))
    :primary  true
    :icon     (icon/add-circle-out)
    :icon-alt (icon/add)}
   {:name     "Show filters"
    :onClick  #(state/update-value [:filterOpen?] true state/form-state-cursor)
    :icon     (icon/filter-list)
    :icon-alt (icon/filter-list)
    :variant  "outlined"
    :color    "default"}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [query]} (state/react state/search-cursor)
        {:keys [loading? filterOpen? filter]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items query)
                            (clojure.core/filter #(if (some? (:state filter))
                                                    (case (:state filter)
                                                      "running" (= "running" (:state %))
                                                      "shutdown" (= 0 (get-in % [:status :tasks :total])))
                                                    true))
                            (sort-by :createdAt)
                            (reverse))]
    (progress/form
      loading?
      (comp/box
        {}
        (common/list "Services"
                     items
                     filtered-items
                     render-metadata
                     onclick-handler
                     toolbar-render-metadata)
        (form-filters filterOpen? filter)))))
