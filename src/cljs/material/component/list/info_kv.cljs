(ns material.component.list.info-kv
  (:require [material.component :as comp]))

(defn list [kv-map]
  (let [last (last kv-map)]
    (comp/list
      {:dense          true
       :disablePadding true}
      (->> kv-map
           (map (fn [item]
                  (comp/list-item
                    {:disableGutters true
                     :divider        (false? (= (:name item)
                                                (:name last)))}
                    (comp/list-item-text
                      {:primary   (:name item)
                       :secondary (:value item)}))))))))


