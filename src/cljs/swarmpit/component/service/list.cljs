(ns swarmpit.component.service.list
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.component.info :as info]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {:predicate ""}))

(def service-list-headers ["Name" "Mode" "Replicas" "Image"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (let [name (fn [item] (get-in item ["Spec" "Name"]))]
    (filter #(string/includes? (name %) predicate) items)))

(defn- service-name [spec index]
  (material/table-row-column
    #js {:key (str "serviceName" index)}
    (get spec "Name")))

(defn- service-mode [spec index]
  (material/table-row-column
    #js {:key (str "serviceMode" index)}
    (let [node (get spec "Mode")
          mode (first (keys node))]
      mode)))

(defn- service-replicas [spec index]
  (material/table-row-column
    #js {:key (str "serviceReplicas" index)}
    (get-in spec ["Mode" "Replicated" "Replicas"])))

(defn- service-image [spec index]
  (material/table-row-column
    #js {:key (str "serviceImage" index)}
    (-> (get-in spec ["TaskTemplate" "ContainerSpec" "Image"])
        (string/split #"\@")
        (first))))

(rum/defc service-list < rum/reactive [items]
  (let [{:keys [predicate]} (rum/react state)
        filtered-items (filter-items items predicate)]
    [:div.list
     [:div.list-action
      [:div.list-action-left
       (material/theme
         (material/text-field
           #js {:hintText       "Filter by name"
                :onChange       (fn [e v] (swap! state assoc :predicate v))
                :underlineStyle #js {:borderColor "rgba(0, 0, 0, 0.2)"}
                :style          #js {:height     "44px"
                                     :lineHeight "15px"}}))]
      [:div.list-action-right
       (material/theme
         (material/raised-button
           #js {:href    "/#/services/create"
                :label   "Create"
                :primary true}))]]

     (material/theme
       (material/table
         #js {:selectable  false
              :onCellClick (fn [i] (print (get (nth filtered-items i) "ID")))}
         (material/table-header-list service-list-headers)
         (material/table-body
           #js {:showRowHover       true
                :displayRowCheckbox false}
           (map-indexed
             (fn [index item]
               (let [spec (get item "Spec")]
                 (material/table-row
                   #js {:key       (str "row" index)
                        :style     #js {:cursor "pointer"}
                        :rowNumber index}
                   (service-name spec index)
                   (service-mode spec index)
                   (service-replicas spec index)
                   (service-image spec index))))
             filtered-items))))]))

(defn mount!
  [items]
  (rum/mount (service-list items) (.getElementById js/document "content")))
