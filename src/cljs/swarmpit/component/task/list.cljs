(ns swarmpit.component.task.list
  (:require [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :task :list])

(def task-list-headers ["Name" "Service" "Image" "Node" "State"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:service %) predicate) items))

(defn- task-list-item
  [item index]
  (material/table-row-column
    #js {:key (str (name (key item)) index)}
    ;(case (key item)
    ;  :serviceId (get services (keyword (val item)))
    ;  :nodeId (get nodes (keyword (val item)))
    ;  (val item))
    (val item)))

(rum/defc task-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        task-id (fn [index] (:id (nth filtered-items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (material/theme
         (material/text-field
           #js {:hintText       "Filter by service name"
                :onChange       (fn [e v]
                                  (state/update-value :predicate v cursor))
                :underlineStyle #js {:borderColor "rgba(0, 0, 0, 0.2)"}
                :style          #js {:height     "44px"
                                     :lineHeight "15px"}}))]]
     (material/theme
       (material/table
         #js {:selectable  false
              :onCellClick (fn [i] (dispatch!
                                     (str "/#/tasks/" (task-id i))))}
         (material/table-header-list task-list-headers)
         (material/table-body
           #js {:showRowHover       true
                :displayRowCheckbox false}
           (map-indexed
             (fn [index item]
               (material/table-row
                 #js {:key       (str "row" index)
                      :style     #js {:cursor "pointer"}
                      :rowNumber index}
                 (->> (select-keys item [:name :service :image :node :state])
                      (map #(task-list-item % index)))))
             filtered-items))))]))

(defn mount!
  [items]
  (rum/mount (task-list items) (.getElementById js/document "content")))
