(ns swarmpit.component.service.info.deployment
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info-kv :as list]
            [sablono.core :refer-macros [html]]
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


    (comp/card
      {:className "Swarmpit-form-card"}
      (comp/card-header
        {:className "Swarmpit-form-card-header"
         :subheader (form/subheader "Deployment" icon/settings)})
      (comp/card-content
        {}
        (html
          [:div {:style {:display        "flex"
                         :flexDirection  "row"
                         :justifyContent "space-between"}}
           [:div
            [:span "Restart policy"]
            [:table
             [:tr
              [:td "CONDITION"]
              [:td {:style {:textAlign "right"}} restart-policy-condition]]
             [:tr
              [:td "DELAY"]
              [:td (str restart-policy-delay "s")]]
             [:tr
              [:td "WINDOW"]
              [:td (str restart-policy-window "s")]]
             [:tr
              [:td "MAX ATTEMPTS"]
              [:td restart-policy-attempts]]]]
           [:div
            [:span "Update policy"]
            [:table
             [:tr
              [:td "PARALLELISM"]
              [:td update-parallelism]]
             [:tr
              [:td "DELAY"]
              [:td (str update-delay "s")]]
             [:tr
              [:td "ORDER"]
              [:td update-order]]
             [:tr
              [:td "ON FAILURE"]
              [:td update-failure-action]]]]])))






    ;[:div.form-layout-group.form-layout-group-border
    ; (form/subsection "Deployment")
    ; (form/item "AUTOREDEPLOY" (if autoredeploy
    ;                             "on"
    ;                             "off"))
    ; (if (not-empty placement)
    ;   [:div
    ;    (form/subsection "Placement")
    ;    (list/table-headless placement
    ;                         placement-render-item
    ;                         placement-render-item-keys)])
    ; (form/subsection "Restart Policy")
    ; (form/item "CONDITION" restart-policy-condition)
    ; (form/item "DELAY" (str restart-policy-delay "s"))
    ; (form/item "WINDOW" (str restart-policy-window "s"))
    ; (form/item "MAX ATTEMPTS" restart-policy-attempts)
    ; (form/subsection "Update Config")
    ; (form/item "PARALLELISM" update-parallelism)
    ; (form/item "DELAY" (str update-delay "s"))
    ; (form/item "ORDER" update-order)
    ; (form/item "ON FAILURE" update-failure-action)
    ; (if (= "rollback" update-failure-action)
    ;   [:div
    ;    (form/subsection "Rollback Config")
    ;    (form/item "PARALLELISM" rollback-parallelism)
    ;    (form/item "DELAY" (str rollback-delay "s"))
    ;    (form/item "ORDER" rollback-order)
    ;    (form/item "ON FAILURE" rollback-failure-action)])]

    ))