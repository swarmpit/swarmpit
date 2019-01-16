(ns swarmpit.component.service.info.deployment
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn item [name value]
  (html
    [:div {:class "Swarmpit-row-space"
           :key   (str "sdcci-" name)}
     [:span name]
     [:span value]]))

(rum/defc form < rum/static [deployment service-id]
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
      {:className "Swarmpit-form-card"
       :key       "sdc"}
      (comp/card-header
        {:className "Swarmpit-form-card-header"
         :key       "sdch"
         :title     (comp/typography
                      {:variant "h6"
                       :key     "deployment-title"} "Deployment")
         :action    (comp/icon-button
                      {:aria-label "Edit"
                       :href       (routes/path-for-frontend
                                     :service-edit
                                     {:id service-id}
                                     {:section "Deployment"})}
                      (comp/svg icon/edit-path))})
      (comp/card-content
        {:key "sdcc"}
        (comp/grid
          {:container true
           :key       "sdcccg"
           :spacing   40}
          (comp/grid
            {:item true
             :key  "sdccggi"
             :xs   6}
            (item "Autoredeploy" (str autoredeploy)))
          (comp/grid
            {:item true
             :key  "sdccplgi"
             :xs   12}
            (form/subsection "Placements")
            (map #(comp/chip {:label (:rule %)
                              :key   (str "lp-" (:rule %))}) placement))
          (comp/grid
            {:item true
             :key  "sdccpogi"
             :xs   12
             :sm   6}
            (form/subsection "Restart Policy")
            (comp/grid
              {:container true
               :key       "sdccpogic"
               :direction "column"}
              (comp/grid
                {:item true
                 :key  "sdccpogicic"}
                (item "Condition" restart-policy-condition))
              (comp/grid
                {:item true
                 :key  "sdccpogicid"}
                (item "Delay" (str restart-policy-delay "s")))
              (comp/grid
                {:item true
                 :key  "sdccpogiciw"}
                (item "Window" (str restart-policy-window "s")))
              (comp/grid
                {:item true
                 :key  "sdccpogicima"}
                (item "Max Attempts" restart-policy-attempts))))
          (comp/grid
            {:item true
             :key  "sdccugi"
             :xs   12
             :sm   6}
            (form/subsection "Update Config")
            (comp/grid
              {:container true
               :key       "sdccugic"
               :direction "column"}
              (comp/grid
                {:item true
                 :key  "sdccugicip"}
                (item "Parallelism" update-parallelism))
              (comp/grid
                {:item true
                 :key  "sdccugicid"}
                (item "Delay" (str update-delay "s")))
              (comp/grid
                {:item true
                 :key  "sdccugicio"}
                (item "Order" update-order))
              (comp/grid
                {:item true
                 :key  "sdccugiciof"}
                (item "On Failure" update-failure-action))))
          (when (= "rollback" (:failureAction update))
            (comp/grid
              {:item true
               :key  "sdccrgi"
               :xs   12
               :sm   6}
              (form/subsection "Rollback Config")
              (comp/grid
                {:container true
                 :key       "sdccrgic"
                 :direction "column"}
                (comp/grid
                  {:item true
                   :key  "sdccrgicip"}
                  (item "Parallelism" rollback-parallelism))
                (comp/grid
                  {:item true
                   :key  "sdccrgicid"}
                  (item "Delay" (str rollback-delay "s")))
                (comp/grid
                  {:item true
                   :key  "sdccrgicio"}
                  (item "Order" rollback-order))
                (comp/grid
                  {:item true
                   :key  "sdccrgiciof"}
                  (item "On Failure" rollback-failure-action))))))))))

