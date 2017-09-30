(ns swarmpit.component.service.info.resources
  (:require [material.component :as comp]
            [rum.core :as rum]))

(rum/defc form < rum/static [{:keys [limit reservation]}]
  (when (or (> limit 0)
            (> reservation 0))
    [:div.form-service-view-group.form-service-group-border
     (comp/form-section "Reservation")
     (comp/form-item "CPU" (:cpu reservation))
     (comp/form-item "MEMORY" (str (:memory reservation) " MB"))
     (comp/form-section "Limit")
     (comp/form-item "CPU" (:cpu limit))
     (comp/form-item "MEMORY" (str (:memory limit) " MB"))]))
