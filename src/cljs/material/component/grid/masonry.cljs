(ns material.component.grid.masonry
  (:require [material.components :as comp]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

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
                      :key  (str "mgchi-" (hash col-content))
                      :xs   12
                      :md   6}
                     (comp/grid
                       {:container true
                        :key       (str "mgchic-" (hash col-content))
                        :spacing   40}
                       (map-indexed
                         (fn [index item]
                           (let [h (hash item)]
                             (comp/grid
                               {:item true
                                :key  (str "mgchics-" index "-" h)
                                :xs   12}
                               (html
                                 [:div {:key (str "mgchicsi-" index "-" h)}
                                  item])))) col-content))))]
    (comp/grid
      {:container true
       :key       "mgpc"
       :spacing   40}
      (col-grid (:first-col colls))
      (col-grid (:second-col colls)))))