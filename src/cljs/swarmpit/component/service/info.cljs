(ns swarmpit.component.service.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-panel-label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(defn- delete-service-handler
  [service-id]
  (handler/delete
    (routes/path-for-backend :service-delete {:id service-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :service-list))
                   (message/info
                     (str "Service " service-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Service removing failed. Reason: " (:error response))))}))

(rum/defc form < rum/static [item]
  (let [id (:id item)
        autoredeloy (get-in item [:deployment :autoredeploy])
        update-delay (get-in item [:deployment :update :delay])
        update-parallelism (get-in item [:deployment :update :parallelism])
        update-failure-action (get-in item [:deployment :update :failureAction])
        rollback-delay (get-in item [:deployment :rollback :delay])
        rollback-parallelism (get-in item [:deployment :rollback :parallelism])
        rollback-failure-action (get-in item [:deployment :rollback :failureAction])
        placement (get-in item [:deployment :placement])
        restart-policy-condition (get-in item [:deployment :restartPolicy :condition])
        restart-policy-delay (get-in item [:deployment :restartPolicy :delay])
        restart-policy-attempts (get-in item [:deployment :restartPolicy :attempts])]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName item)
                        (comp/label-info
                          (form-panel-label item)))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :service-edit {:id id})
            :label   "Edit"
            :primary true}))
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(delete-service-handler id)
            :label      "Delete"}))]]
     [:div.form-view
      [:div.form-view-group
       (comp/form-section "General settings")
       (comp/form-item "ID" id)
       (comp/form-item "SERVICE NAME" (:serviceName item))
       (comp/form-item "CREATED" (:createdAt item))
       (comp/form-item "LAST UPDATE" (:updatedAt item))
       (comp/form-item "IMAGE" (get-in item [:repository :image]))
       (comp/form-item "IMAGE DIGEST" (get-in item [:repository :imageDigest]))
       (comp/form-item "MODE" (:mode item))]
      [:div.form-view-group
       (comp/form-section "Ports")
       (ports/form-view (:ports item))]
      [:div.form-view-group
       (comp/form-section "Networks")
       (networks/form-view (:networks item))]
      [:div.form-view-group
       (comp/form-section "Mounts")
       (mounts/form-view (:mounts item))]
      [:div.form-view-group
       (comp/form-section "Secrets")
       (secrets/form-view (:secrets item))]
      [:div.form-view-group
       (comp/form-section "Environment variables")
       (variables/form-view (:variables item))]
      [:div.form-view-group
       (comp/form-section "Deployment")
       (comp/form-item "AUTOREDEPLOY" (if autoredeloy
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
          (placement/form-view placement)])]
      [:div.form-view-group
       (comp/form-section "Tasks")
       (comp/list-table tasks/headers
                        (filter #(not (= "shutdown" (:state %))) (:tasks item))
                        tasks/render-item
                        tasks/render-item-keys
                        tasks/onclick-handler)]]]))