(ns swarmpit.component.service.info.deployment
  (:require [material.component :as comp]
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
        update-failure-action (get-in deployment [:update :failureAction])
        rollback-delay (get-in deployment [:rollback :delay])
        rollback-parallelism (get-in deployment [:rollback :parallelism])
        rollback-failure-action (get-in deployment [:rollback :failureAction])
        placement (:placement deployment)
        restart-policy-condition (get-in deployment [:restartPolicy :condition])
        restart-policy-delay (get-in deployment [:restartPolicy :delay])
        restart-policy-attempts (get-in deployment [:restartPolicy :attempts])]
    [:div.form-service-view-group.form-service-group-border
     (comp/form-section "Deployment")
     (comp/form-item "AUTOREDEPLOY" (if autoredeploy
                                      "on"
                                      "off"))
     (comp/form-subsection "Restart Policy")
     (comp/form-item "CONDITION" restart-policy-condition)
     (comp/form-item "DELAY" (str restart-policy-delay "s"))
     (comp/form-item "MAX ATTEMPTS" restart-policy-attempts)
     (comp/form-subsection "Update Config")
     (comp/form-item "PARALLELISM" update-parallelism)
     (comp/form-item "DELAY" (str update-delay "s"))
     (comp/form-item "ON FAILURE" update-failure-action)
     (if (= "rollback" update-failure-action)
       [:div
        (comp/form-subsection "Rollback Config")
        (comp/form-item "PARALLELISM" rollback-parallelism)
        (comp/form-item "DELAY" (str rollback-delay "s"))
        (comp/form-item "ON FAILURE" rollback-failure-action)])
     (if (not-empty placement)
       [:div
        (comp/form-subsection "Placement")
        (comp/form-info-table-headless placement
                                       placement-render-item
                                       placement-render-item-keys)])]))