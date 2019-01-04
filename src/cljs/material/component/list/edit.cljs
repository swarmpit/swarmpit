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
    {:key            (str "list-in-" index)
     :className      "Swarmpit-list-item-edit"
     :disableGutters true}
    (->> (select-keys* item (render-keys render-metadata))
         (map-indexed
           (fn [coll-index coll]
             (let [render-fn (:render-fn (nth render-metadata coll-index))
                   value (val coll)]
               (render-fn value item index)))))
    (cmp/list-item-secondary-action
      {:key (str "list-ins-" index)}
      (cmp/tooltip
        {:key       (str "list-inst-" index)
         :title     "Delete"
         :placement "top-start"}
        (cmp/icon-button
          {:color   "secondary"
           :onClick #(delete-handler-fn index)}
          (cmp/svg icon/trash-path))))))

(defn list-item-small [render-metadata index item delete-handler-fn]
  (cmp/list-item
    {:key            (str "list-is-" index)
     :className      "Swarmpit-list-item-edit-small"
     :disableGutters true}
    (html
      [:div {:key (str "list-isb-" index)}
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
    [:div {:key (str "list-i-" index)}
     (cmp/hidden
       {:only           ["xs" "sm"]
        :implementation "js"}
       (list-item-normal render-metadata index item delete-handler-fn))
     (cmp/hidden
       {:only           ["md" "lg" "xl"]
        :implementation "js"}
       (list-item-small render-metadata index item delete-handler-fn))]))

(rum/defc list < rum/static
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

