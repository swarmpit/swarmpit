(ns swarmpit.component.service.list
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.router :as router]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {:predicate ""}))

(def service-list-headers ["Name" "Mode" "Replicas" "Image"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:serviceName %) predicate) items))

(defn- service-list-item
  [item key index]
  (material/table-row-column
    #js {:key (str (name key) index)}
    (key item)))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [predicate]} (rum/react state)
        filtered-items (filter-items items predicate)
        service-id (fn [index] (:id (nth filtered-items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (material/theme
         (material/text-field
           #js {:hintText       "Filter by name"
                :onChange       (fn [e v] (swap! state assoc :predicate v))
                :underlineStyle #js {:borderColor "rgba(0, 0, 0, 0.2)"}
                :style          #js {:height     "44px"
                                     :lineHeight "15px"}}))]
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:href    "/#/services/create"
                :label   "Create"
                :primary true}))]]
     (material/theme
       (material/table
         #js {:selectable  false
              :onCellClick (fn [i] (router/dispatch!
                                     (str "/#/services/" (service-id i))))}
         (material/table-header-list service-list-headers)
         (material/table-body
           #js {:showRowHover       true
                :displayRowCheckbox false}
           (map-indexed
             (fn [index item]
               (material/table-row
                 #js {:key       (str "row" index)
                      :style     #js {:cursor "pointer"}
                      :rowNumber index}
                 (->> [:serviceName :mode :replicas :image]
                      (map #(service-list-item item % index)))))
             filtered-items))))]))

(defn mount!
  [items]
  (rum/mount (service-list items) (.getElementById js/document "content")))