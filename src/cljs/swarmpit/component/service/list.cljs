(ns swarmpit.component.service.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :list :filter])

(def headers ["Name" "Image" "Mode" "Replicas" "Status" "Updates"])

(def form-replicas-style
  {:backgroundColor "rgb(245, 245, 245)"
   :border          "1px solid rgb(224, 228, 231)"})

(def form-replicas-label-style
  {:fontSize "13px"})

(defn- form-replicas [value]
  (comp/chip {:style      form-replicas-style
              :labelStyle form-replicas-label-style} value))

(defn- form-updates [value]
  (let [status (if value "loading" "ready")]
    (comp/loader
      {:status status
       :top    0})))

(defn- form-state [value]
  (case value
    "running" (comp/label-green value)
    "not running" (comp/label-grey value)
    "partly running" (comp/label-yellow value)))

(defn- filter-items
  "Filter list `items` based on `name` & `cranky?` flag"
  [items name cranky?]
  (let [is-running (fn [item] (= "running" (:state item)))
        is-updating (fn [item] (get-in item [:status :update]))]
    (if cranky?
      (filter #(and (string/includes? (:serviceName %) name)
                    (and (not (is-running %))
                         (not (is-updating %)))) items)
      (filter #(string/includes? (:serviceName %) name) items))))

(def render-item-keys
  [[:serviceName] [:repository :image] [:mode] [:status :info] [:state] [:status :update]])

(defn- render-item
  [item]
  (let [value (val item)]
    (case (key item)
      :state (form-state value)
      :info (form-replicas value)
      :update (form-updates value)
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :service-info (select-keys item [:id])))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [serviceName cranky]} (state/react cursor)
        filtered-items (filter-items items serviceName cranky)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:serviceName] v cursor))})
       [:span.form-panel-space]
       (comp/panel-checkbox
         {:checked cranky
          :label   "Show cranky services"
          :onCheck (fn [_ v]
                     (state/update-value [:cranky] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :service-create-image)
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      onclick-handler)]))

(defn- init-state
  []
  (state/set-value {:serviceName ""
                    :cranky      false} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (service-list items) (.getElementById js/document "content")))