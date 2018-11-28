(ns material.component.grid.masonry
  (:require [material.component :as comp]))

(defn grid
  [{:keys [:first-col-pred] :or {first-col-pred even?} :as opts} & body]
  {:pre [(map? opts)]}
  (let [body' (remove nil? body)
        colls (reduce
                (fn [acc val]
                  (-> (if (first-col-pred (:idx acc))
                        (update acc :first-col conj val)
                        (update acc :second-col conj val))
                      (update :idx inc)))
                {:idx 0 :first-col [] :second-col []}
                body')
        col-grid (fn [col-content]
                   (comp/grid
                     {:item true
                      :xs   12
                      :md   6}
                     (comp/grid
                       {:container true
                        :spacing   40}
                       (map
                         #(comp/grid
                            {:item true
                             :xs   12} %)
                         col-content))))]
    (comp/grid
      {:container true
       :spacing   40}
      (col-grid (:first-col colls))
      (col-grid (:second-col colls)))))