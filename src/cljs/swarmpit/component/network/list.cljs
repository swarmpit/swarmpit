(ns swarmpit.component.network.list
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.router :as router]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {:predicate ""}))

(def network-list-headers ["Name" "Driver" "Internal"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- network-list-item
  [item index]
  (material/table-row-column
    #js {:key (str (name (key item)) index)}
    (case (val item)
      true "yes"
      false "no"
      (val item))))

(rum/defc network-list < rum/reactive [items]
  (let [{:keys [predicate]} (rum/react state)
        filtered-items (filter-items items predicate)
        network-id (fn [index] (:id (nth filtered-items index)))]
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
           #js {:href    "/#/networks/create"
                :label   "Create"
                :primary true}))]]
     (material/theme
       (material/table
         #js {:selectable  false
              :onCellClick (fn [i] (router/dispatch!
                                     (str "/#/networks/" (network-id i))))}
         (material/table-header-list network-list-headers)
         (material/table-body
           #js {:showRowHover       true
                :displayRowCheckbox false}
           (map-indexed
             (fn [index item]
               (material/table-row
                 #js {:key       (str "row" index)
                      :style     #js {:cursor "pointer"}
                      :rowNumber index}
                 (->> (select-keys item [:name :driver :internal])
                      (map #(network-list-item % index)))))
             filtered-items))))]))

(defn mount!
  [items]
  (rum/mount (network-list items) (.getElementById js/document "content")))