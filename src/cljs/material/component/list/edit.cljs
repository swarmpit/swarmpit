(ns material.component.list.edit
  (:refer-clojure :exclude [list])
  (:require [material.icon :as icon]
            [material.components :as cmp]
            [material.component.list.util :refer [render-keys]]
            [swarmpit.utils :refer [select-keys*]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn list-item-normal [render-metadata index item delete-handler-fn]
  (cmp/list-item
    {:key            (str "list-in-" (:key item))
     :className      "Swarmpit-list-item-edit"
     :disableGutters true}
    (->> (select-keys* item (render-keys render-metadata))
         (map-indexed
           (fn [coll-index coll]
             (let [render-fn (:render-fn (nth render-metadata coll-index))
                   value (val coll)]
               (render-fn value item index)))))
    (cmp/list-item-secondary-action
      {:key (str "list-ins-" (:key item))}
      (cmp/tooltip
        {:key       (str "list-inst-" (:key item))
         :title     "Delete"
         :placement "top-start"}
        (cmp/icon-button
          {:color   "primary"
           :onClick #(delete-handler-fn index)}
          (cmp/svg icon/trash-path))))))

(defn list-item-small [render-metadata index item delete-handler-fn]
  (cmp/list-item
    {:key            (str "list-is-" (:key item))
     :className      "Swarmpit-list-item-edit-small"
     :disableGutters true}
    (html
      [:div {:key (str "list-isb-" (:key item))}
       (->> (select-keys* item (render-keys render-metadata))
            (map-indexed
              (fn [coll-index coll]
                (let [render-fn (:render-fn (nth render-metadata coll-index))
                      value (val coll)]
                  (render-fn value item index)))))
       (cmp/button {:size    "small"
                    :onClick #(delete-handler-fn index)
                    :color   "primary"} "Delete")])))

(defn list-item
  [render-metadata index item delete-handler-fn]
  (html
    [:div {:key (str "list-i-" (:key item))}
     (cmp/hidden
       {:only           ["xs" "sm"]
        :implementation "js"}
       (list-item-normal render-metadata index item delete-handler-fn))
     (cmp/hidden
       {:only           ["md" "lg" "xl"]
        :implementation "js"}
       (list-item-small render-metadata index item delete-handler-fn))]))

(rum/defc list < rum/reactive [render-metadata items delete-handler-fn]
  (cmp/list
    {:dense true}
    (map-indexed
      (fn [index item]
        (list-item
          render-metadata
          index
          item
          delete-handler-fn)) items)))