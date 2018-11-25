(ns material.component.list.edit
  (:require [material.icon :as icon]
            [material.component :as cmp]
            [material.component.list.util :refer [render-keys]]
            [swarmpit.utils :refer [select-keys*]]
            [sablono.core :refer-macros [html]]))

(defn list-item
  [render-metadata index item delete-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (cmp/list-item
         {:key            (str "Swarmpit-list-item-" index)
          :className      "Swarmpit-list-item-edit"
          :disableGutters true}
         (->> (select-keys* item (render-keys render-metadata))
              (map-indexed
                (fn [coll-index coll]
                  (let [render-fn (:render-fn (nth render-metadata coll-index))
                        value (val coll)]
                    (render-fn value item index)))))
         (cmp/list-item-secondary-action
           {:key (str "Swarmpit-list-status-" index)}
           (cmp/tooltip
             {:title     "Delete"
              :placement "top-start"}
             (cmp/icon-button
               {:color   "secondary"
                :onClick #(delete-handler-fn index)}
               (cmp/svg icon/trash))))))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (cmp/list-item
         {:key            (str "Swarmpit-list-item-" index)
          :className      "Swarmpit-list-item-edit-small"
          :disableGutters true}
         (html
           [:div
            (->> (select-keys* item (render-keys render-metadata))
                 (map-indexed
                   (fn [coll-index coll]
                     (let [render-fn (:render-fn (nth render-metadata coll-index))
                           value (val coll)]
                       (render-fn value item index)))))
            (cmp/button {:size    "small"
                         :onClick #(delete-handler-fn index)
                         :color   "primary"} "Delete")])))]))

(defn list
  [render-metadata items delete-handler-fn]
  (cmp/list
    {:dense true}
    (map-indexed
      (fn [index item]
        (list-item
          render-metadata
          index
          item
          delete-handler-fn)) items)))

