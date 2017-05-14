(ns swarmpit.component.node.list
  (:require [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [swarmpit.router :as router]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :node :list])

(def node-list-headers ["Name" "Status" "Availability" "Leader"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- node-list-item
  [item index]
  (material/table-row-column
    #js {:key (str (name (key item)) index)}
    (case (val item)
      true "yes"
      false "no"
      (val item))))

(rum/defc node-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        node-id (fn [index] (:id (nth filtered-items index)))]
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
                                     :lineHeight "15px"}}))]]
     (material/theme
       (material/table
         #js {:selectable  false
              :onCellClick (fn [i] (router/dispatch!
                                     (str "/#/nodes/" (node-id i))))}
         (material/table-header-list node-list-headers)
         (material/table-body
           #js {:showRowHover       true
                :displayRowCheckbox false}
           (map-indexed
             (fn [index item]
               (material/table-row
                 #js {:key       (str "row" index)
                      :style     #js {:cursor "pointer"}
                      :rowNumber index}
                 (->> (select-keys item [:name :state :availability :leader])
                      (map #(node-list-item % index)))))
             filtered-items))))]))

(defn mount!
  [items]
  (rum/mount (node-list items) (.getElementById js/document "content")))
