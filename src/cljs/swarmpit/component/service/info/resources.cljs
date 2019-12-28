(ns swarmpit.component.service.info.resources
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn format-cpu
  [value]
  (if (> value 0)
    value
    "n/a"))

(defn format-memory
  [value]
  (if (> value 0)
    (str value " MiB")
    "n/a"))

(rum/defc form < rum/static [{:keys [limit reservation]} service-id immutable?]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :title     (comp/typography {:variant "h6"} "Resources")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :disabled   immutable?
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section 3})}
                    (comp/svg icon/edit-path))})
    (comp/card-content
      {}
      (comp/grid
        {:container true
         :spacing   5}
        (comp/grid
          {:item true
           :xs   12
           :sm   6}
          (form/subsection "CPU")
          (comp/grid
            {:container true
             :direction "column"}
            (comp/grid
              {:item true}
              (form/item "Reservation" (format-cpu (:cpu reservation))))
            (comp/grid
              {:item true}
              (form/item "Limit" (format-cpu (:cpu limit))))))
        (comp/grid
          {:item true
           :xs   12
           :sm   6}
          (form/subsection "Memory")
          (comp/grid
            {:container true
             :direction "column"}
            (comp/grid
              {:item true}
              (form/item "Reservation" (format-memory (:memory reservation))))
            (comp/grid
              {:item true}
              (form/item "Limit" (format-memory (:memory limit))))))))))
