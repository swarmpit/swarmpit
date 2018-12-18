(ns swarmpit.component.service.info.resources
  (:require [material.component.form :as form]
            [rum.core :as rum]))

(defn provided?
  [resource]
  (let [cpu (:cpu resource)
        memory (:memory resource)]
    (or (> cpu 0)
        (> memory 0))))

(rum/defc form-resource < rum/static [resource label]
  [:div
   (form/subsection label)
   ;(form/item "CPU" (:cpu resource))
   ;(form/item "MEMORY" (str (:memory resource) " MB"))

   ])

(rum/defc form < rum/static [{:keys [limit reservation]}]
  (cond
    (and (provided? reservation)
         (provided? limit))
    [:div.form-layout-group.form-layout-group-border
     (form/subsection "Resource")
     (form-resource reservation "Reservation")
     (form-resource limit "Limit")]
    (provided? reservation)
    [:div.form-layout-group.form-layout-group-border
     (form/subsection "Resource")
     (form-resource reservation "Reservation")]
    (provided? limit)
    [:div.form-layout-group.form-layout-group-border
     (form/subsection "Resource")
     (form-resource limit "Limit")]))
