(ns swarmpit.component.service.list
  (:require [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :list])

(def service-list-headers ["Name" "Mode" "Replicas" "Image"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:serviceName %) predicate) items))

(defn- service-list-item
  [item index]
  (material/table-row-column
    #js {:key (str (name (key item)) index)}
    (val item)))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        service-id (fn [index] (:id (nth filtered-items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (material/theme
         (material/text-field
           #js {:hintText       "Filter by name"
                :onChange       (fn [e v]
                                  (state/update-value :predicate v cursor))
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
              :onCellClick (fn [i] (dispatch!
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
                 (->> (select-keys item [:serviceName :mode :replicas :image])
                      (map #(service-list-item % index)))))
             filtered-items))))]))

(defn mount!
  [items]
  (rum/mount (service-list items) (.getElementById js/document "content")))