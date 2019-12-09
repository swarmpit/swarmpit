(ns swarmpit.component.service.info.deployment
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [deployment service-id immutable?]
  (let [autoredeploy (:autoredeploy deployment)
        update-delay (get-in deployment [:update :delay])
        update-parallelism (get-in deployment [:update :parallelism])
        update-order (get-in deployment [:update :order])
        update-failure-action (get-in deployment [:update :failureAction])
        rollback-delay (get-in deployment [:rollback :delay])
        rollback-parallelism (get-in deployment [:rollback :parallelism])
        rollback-order (get-in deployment [:rollback :order])
        rollback-failure-action (get-in deployment [:rollback :failureAction])
        placement (:placement deployment)
        restart-policy-condition (get-in deployment [:restartPolicy :condition])
        restart-policy-delay (get-in deployment [:restartPolicy :delay])
        restart-policy-window (get-in deployment [:restartPolicy :window])
        restart-policy-attempts (get-in deployment [:restartPolicy :attempts])]
    (comp/card
      {:className "Swarmpit-form-card"}
      (comp/card-header
        {:className "Swarmpit-form-card-header"
         :title     (comp/typography {:variant "h6"} "Deployment")
         :action    (comp/icon-button
                      {:aria-label "Edit"
                       :disabled   immutable?
                       :href       (routes/path-for-frontend
                                     :service-edit
                                     {:id service-id}
                                     {:section 4})}
                      (comp/svg icon/edit-path))})
      (comp/card-content
        {}
        (comp/grid
          {:container true
           :spacing   5}
          (comp/grid
            {:item true
             :xs   6}
            (form/item "Autoredeploy" (str autoredeploy)))
          (comp/grid
            {:item true
             :xs   12}
            (form/subsection "Placements")
            (if (empty? placement)
              (form/item-info "No placement constraints defined.")
              (map #(comp/chip {:label (:rule %)
                                :key   (str "lp-" (:rule %))}) placement)))
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (form/subsection "Restart Policy")
            (comp/grid
              {:container true
               :direction "column"}
              (comp/grid
                {:item true}
                (form/item "Condition" restart-policy-condition))
              (comp/grid
                {:item true}
                (form/item "Delay" (str restart-policy-delay "s")))
              (comp/grid
                {:item true}
                (form/item "Window" (str restart-policy-window "s")))
              (comp/grid
                {:item true}
                (form/item "Max Attempts" restart-policy-attempts))))
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (form/subsection "Update Config")
            (comp/grid
              {:container true
               :direction "column"}
              (comp/grid
                {:item true}
                (form/item "Parallelism" update-parallelism))
              (comp/grid
                {:item true}
                (form/item "Delay" (str update-delay "s")))
              (comp/grid
                {:item true}
                (form/item "Order" update-order))
              (comp/grid
                {:item true}
                (form/item "On Failure" update-failure-action))))
          (when (= "rollback" (:failureAction update))
            (comp/grid
              {:item true
               :xs   12
               :sm   6}
              (form/subsection "Rollback Config")
              (comp/grid
                {:container true
                 :direction "column"}
                (comp/grid
                  {:item true}
                  (form/item "Parallelism" rollback-parallelism))
                (comp/grid
                  {:item true}
                  (form/item "Delay" (str rollback-delay "s")))
                (comp/grid
                  {:item true}
                  (form/item "Order" rollback-order))
                (comp/grid
                  {:item true}
                  (form/item "On Failure" rollback-failure-action))))))))))

