(ns swarmpit.component.service.info.resources
  (:require [material.component :as comp]
            [rum.core :as rum]))

(defn provided?
  [resource]
  (let [cpu (:cpu resource)
        memory (:memory resource)]
    (or (some? cpu)
        (some? memory))))

(rum/defc form-resource < rum/static [resource label]
  [:div
   (comp/form-section label)
   (comp/form-item "CPU" (:cpu resource))
   (comp/form-item "MEMORY" (str (:memory resource) " MB"))])

(rum/defc form < rum/static [{:keys [limit reservation]}]
  (cond
    (and (provided? reservation)
         (provided? limit))
    [:div.form-service-view-group.form-service-group-border
     (form-resource reservation "Reservation")
     (form-resource limit "Limit")]
    (provided? reservation)
    [:div.form-service-view-group.form-service-group-border
     (form-resource reservation "Reservation")]
    (provided? limit)
    [:div.form-service-view-group.form-service-group-border
     (form-resource limit "Limit")]))
