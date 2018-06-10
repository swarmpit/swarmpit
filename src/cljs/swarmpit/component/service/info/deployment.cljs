(ns swarmpit.component.service.info.deployment
  (:require [material.component.form :as form]
            [material.component.list-table-info :as list]
            [rum.core :as rum]))

(enable-console-print!)

(def placement-render-item-keys
  [[:rule]])

(defn- placement-render-item
  [item]
  (val item))

(rum/defc form < rum/static [deployment]
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
    [:div.form-layout-group.form-layout-group-border
     (form/section "Deployment")
     (form/item "AUTOREDEPLOY" (if autoredeploy
                                 "on"
                                 "off"))
     (if (not-empty placement)
       [:div
        (form/subsection "Placement")
        (list/table-headless placement
                             placement-render-item
                             placement-render-item-keys)])
     (form/subsection "Restart Policy")
     (form/item "CONDITION" restart-policy-condition)
     (form/item "DELAY" (str restart-policy-delay "s"))
     (form/item "WINDOW" (str restart-policy-window "s"))
     (form/item "MAX ATTEMPTS" restart-policy-attempts)
     (form/subsection "Update Config")
     (form/item "PARALLELISM" update-parallelism)
     (form/item "DELAY" (str update-delay "s"))
     (form/item "ORDER" update-order)
     (form/item "ON FAILURE" update-failure-action)
     (if (= "rollback" update-failure-action)
       [:div
        (form/subsection "Rollback Config")
        (form/item "PARALLELISM" rollback-parallelism)
        (form/item "DELAY" (str rollback-delay "s"))
        (form/item "ORDER" rollback-order)
        (form/item "ON FAILURE" rollback-failure-action)])]))