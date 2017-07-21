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
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defonce service-tasks (atom nil))

(defonce service-networks (atom nil))

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

(rum/defc form-settings < rum/static [service]
  [:div.form-view-group
   (comp/form-section "General settings")
   (comp/form-item "ID" (:id service))
   (comp/form-item "SERVICE NAME" (:serviceName service))
   (comp/form-item "CREATED" (:createdAt service))
   (comp/form-item "LAST UPDATE" (:updatedAt service))
   (comp/form-item "IMAGE" (get-in service [:repository :image]))
   (comp/form-item "IMAGE DIGEST" (get-in service [:repository :imageDigest]))
   (comp/form-item "MODE" (:mode service))])

(rum/defc form-ports < rum/static [ports]
  (when (not-empty ports)
    [:div.form-view-group
     (comp/form-section "Ports")
     (comp/list-table-auto ports/headers
                           ports
                           ports/render-item
                           ports/render-item-keys
                           nil)]))

(rum/defc form-networks < rum/static [networks]
  (when (not-empty networks)
    [:div.form-view-group
     (comp/form-section "Networks")
     (comp/list-table-auto networks/headers
                           networks
                           networks/render-item
                           networks/render-item-keys
                           networks/onclick-handler)]))

(rum/defc form-mounts < rum/static [mounts]
  (when (not-empty mounts)
    [:div.form-view-group
     (comp/form-section "Mounts")
     (comp/list-table-auto mounts/headers
                           mounts
                           mounts/render-item
                           mounts/render-item-keys
                           nil)]))

(rum/defc form-secrets < rum/static [secrets]
  (when (not-empty secrets)
    [:div.form-view-group
     (comp/form-section "Secrets")
     (comp/list-table-auto secrets/headers
                           secrets
                           secrets/render-item
                           secrets/render-item-keys
                           secrets/onclick-handler)]))

(rum/defc form-variables < rum/static [variables]
  (when (not-empty variables)
    [:div.form-view-group
     (comp/form-section "Environment variables")
     (comp/list-table-auto variables/headers
                           variables
                           variables/render-item
                           variables/render-item-keys
                           nil)]))

(rum/defc form-labels < rum/static [labels]
  (when (not-empty labels)
    [:div.form-view-group
     (comp/form-section "Labels")
     (comp/list-table-auto labels/headers
                           labels
                           labels/render-item
                           labels/render-item-keys
                           nil)]))

(rum/defc form-deployment < rum/static [deployment]
  (let [autoredeloy (:autoredeploy deployment)
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
        (placement/form-view placement)])]))

(rum/defc form-tasks < rum/static [tasks]
  [:div.form-view-group
   (comp/form-section "Tasks")
   (comp/list-table-auto tasks/headers
                         (filter #(not (= "shutdown" (:state %))) tasks)
                         tasks/render-item
                         tasks/render-item-keys
                         tasks/onclick-handler)])

(rum/defc form < rum/static [data]
  (let [{:keys [service networks tasks]} data
        ports (:ports service)
        mounts (:mounts service)
        secrets (:secrets service)
        variables (:variables service)
        labels (:labels service)
        deployment (:deployment service)
        id (:id service)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName service)
                        (comp/label-info
                          (form-panel-label service)))]
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
      (form-settings service)
      (form-ports ports)
      (form-networks networks)
      (form-mounts mounts)
      (form-secrets secrets)
      (form-variables variables)
      (form-labels labels)
      (form-deployment deployment)
      (form-tasks tasks)]]))