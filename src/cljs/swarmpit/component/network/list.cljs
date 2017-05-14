(ns swarmpit.component.network.list
  (:require [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :network :list])

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
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        network-id (fn [index] (:id (nth filtered-items index)))]
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
           #js {:href    "/#/networks/create"
                :label   "Create"
                :primary true}))]]
     (material/theme
       (material/table
         #js {:selectable  false
              :onCellClick (fn [i] (dispatch!
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