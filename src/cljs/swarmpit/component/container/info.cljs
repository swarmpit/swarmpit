(ns swarmpit.component.container.info
  (:require [clojure.string :as str]
            [material.components :as comp]
            [material.icon :as icon]
            [material.component.label :as label]
            [material.component.list.basic :as list]
            [material.component.form :as form]
            [swarmpit.component.container.list :as containers]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn- container-handler
  [id]
  (when-not (or (state/get-value (conj state/form-state-cursor :loading?))
                (state/get-value (conj state/form-state-cursor :refreshing?)))
    (ajax/get
      (routes/path-for-backend :container {:id id})
      {:state (if (some? (state/get-value (conj state/form-value-cursor :container)))
                [:refreshing?] [:loading?])
       :on-success (fn [{:keys [response origin?]}]
                     (when origin?
                       (state/update-value [:container] response state/form-value-cursor)))})))

(defn- logs-handler
  [id]
  (when-not (state/get-value (conj state/form-state-cursor :logs-loading?))
    (ajax/get
      (routes/path-for-backend :container-logs {:id id} {:tail 200})
      {:state [:logs-loading?]
       :on-success (fn [{:keys [response origin?]}]
                     (when origin?
                       (state/update-value [:logs] (:logs response) state/form-value-cursor)
                       (state/update-value [:logs-error] nil state/form-state-cursor)))
       :on-error (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:logs-error] (or (:error response) "Unable to fetch container logs.") state/form-state-cursor)))})))

(defn- action-handler
  [id action]
  (let [delete? (= "delete" action)]
    ((if delete? ajax/delete ajax/post)
     (if delete?
       (routes/path-for-backend :container {:id id})
       (routes/path-for-backend :container-action {:id id :action action}))
     {:state [:processing?]
      :on-success (fn [{:keys [origin?]}]
                    (when origin?
                      (if delete?
                        (do (dispatch! (routes/path-for-frontend :container-list))
                            (message/info "Container removed."))
                        (container-handler id))))})))

(defn- confirm-action
  [action title]
  (state/update-value [:action] action state/form-state-cursor)
  (state/update-value [:action-title] title state/form-state-cursor)
  (state/update-value [:open] true dialog/dialog-cursor))

(defn- actions
  [{:keys [state managed]} processing?]
  (when (and (storage/user?) (not managed))
    (let [action (fn [value title icon]
                   {:name title :icon icon :disabled processing?
                    :onClick #(confirm-action value title)})
          start (action "start" "Start" (icon/play {}))
          stop (action "stop" "Stop" (icon/stop {}))
          delete (assoc (action "delete" "Delete" (comp/svg icon/trash-path))
                        :variant "outlined" :color "default")]
      (case state
        ("created" "exited") [start delete]
        "running" [(assoc (action "restart" "Restart" (icon/sync {})) :main true :group true)
                   (assoc stop :group true)
                   (assoc (action "pause" "Pause" (icon/pause {})) :group true)]
        "paused" [(action "unpause" "Resume" (icon/play {}))]
        "restarting" [stop]
        "dead" [delete]
        []))))

(defn- published-port [{:keys [hostIp hostPort]}]
  (when hostPort
    (str (if (str/includes? (or hostIp "") ":") (str "[" hostIp "]") hostIp)
         ":" hostPort)))

(def ports-metadata
  {:table {:summary [{:name "Container port" :render-fn :containerPort}
                     {:name "Host port" :render-fn published-port}]}
   :list {:primary :containerPort :secondary published-port}})

(def networks-metadata
  {:table {:summary [{:name "Network" :render-fn :networkName}
                     {:name "Address" :render-fn :address}]}
   :list {:primary :networkName :secondary :address}})

(def mounts-metadata
  {:table {:summary [{:name "Container path" :render-fn :target}
                     {:name "Source" :render-fn #(or (:volumeName %) (:source %))}
                     {:name "Type" :render-fn :type}
                     {:name "Read only" :render-fn #(when (:readOnly %) "yes")}]}
   :list {:primary :target :secondary #(or (:volumeName %) (:source %))}})

(rum/defc resource-card < rum/static [title items metadata onclick]
  (when (seq items)
    (comp/grid {:item true :xs 12}
      (comp/card {:className "Swarmpit-card"}
        (comp/card-header {:className "Swarmpit-table-card-header"
                           :title (comp/typography {:variant "h6"} title)})
        (comp/card-content {:className "Swarmpit-table-card-content"}
          (list/responsive metadata items onclick))))))

(rum/defc details < rum/static [{:keys [id image state createdAt startedAt finishedAt exitCode error
                                      health restartPolicy command nodeId nodeName serviceId taskId project]
                               :as container}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:subheader (form/item-labels
                    [(containers/container-state container)
                     (label/base (containers/owner container) "info")
                     (when health
                       (label/base health (case health "healthy" "green" "unhealthy" "red" "yellow")))])})
    (when (seq error)
      (comp/card-content {} (form/error-message error)))
    (form/item-main "ID" id false)
    (form/item-main "Image" image)
    (when nodeName
      (form/item-main "Node" (if nodeId (containers/resource-link nodeName :node-info {:id nodeId}) nodeName)))
    (when project (form/item-main "Project" project))
    (form/item-main "Created" (form/item-date createdAt))
    (when (and startedAt (not= "0001-01-01T00:00:00Z" startedAt))
      (form/item-main "Started" (form/item-date startedAt)))
    (when (contains? #{"exited" "dead"} state)
      (comp/box {}
        (form/item-main "Finished" (form/item-date finishedAt))
        (form/item-main "Exit code" exitCode)))
    (form/item-main "Restart policy" (or (not-empty restartPolicy) "no"))
    (when (seq command) (form/item-main "Command" (str/join " " command)))
    (comp/divider {})
    (comp/card-actions
      {:style {:flexWrap "wrap"}}
      (when serviceId
        (comp/button {:size "small" :color "primary"
                       :href (routes/path-for-frontend :service-info {:id serviceId})} "See service"))
      (when taskId
        (comp/button {:size "small" :color "primary"
                       :href (routes/path-for-frontend :task-info {:id taskId})} "See task"))
      (comp/button
        {:size "small" :color "primary"
         :onClick (fn []
                    (state/update-value [:logs-open?] true state/form-state-cursor)
                    (logs-handler id))}
        "View log"))))

(rum/defc logs < rum/static [name content {:keys [logs-open? logs-loading? logs-error]}]
  (let [close #(state/update-value [:logs-open?] false state/form-state-cursor)]
    (comp/dialog
      {:fullScreen true :open (true? logs-open?) :onClose close
       :aria-labelledby "container-log-title"}
      (comp/app-bar {:color "primary" :className "Swarmpit-log-appbar"}
        (comp/toolbar {}
          (comp/icon-button {:color "inherit" :aria-label "Close logs" :onClick close} (icon/close {}))
          (comp/typography {:variant "h6" :className "Swarmpit-log-title" :noWrap true} "Container")
          (comp/typography {:id "container-log-title" :variant "h6" :noWrap true} name)))
      (when (and logs-loading? (nil? content))
        (comp/linear-progress {:className "Swarmpit-log-progress"}))
      (html
        [:div.Swarmpit-log {:style {:overflowWrap "anywhere"}}
         [:div.Swarmpit-toolbar]
         (if logs-error
           (form/error-message logs-error)
           [:div (or (not-empty content) (when-not logs-loading? "No logs available."))])]))))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (state/set-value {:loading? false :logs-open? false} state/form-state-cursor)
      (state/update-value [:open] false dialog/dialog-cursor)
      (container-handler id))))

(def mixin-refresh-form
  (mixin/refresh-form
    (fn [{{:keys [id]} :params}]
      (when-not (state/get-value (conj state/form-state-cursor :processing?))
        (container-handler id))
      (when (state/get-value (conj state/form-state-cursor :logs-open?))
        (logs-handler id))) 5000))

(rum/defc form < rum/reactive mixin-init-form mixin-refresh-form [_]
  (let [{:keys [container] log-content :logs} (state/react state/form-value-cursor)
        {:keys [loading? processing? action action-title] :as form-state} (state/react state/form-state-cursor)
        {:keys [id containerName mounts networks ports]} container
        resources? (some seq [mounts networks ports])]
    (progress/form
      loading?
      (when container
        (comp/mui
          (html
            [:div.Swarmpit-form
             (dialog/confirm-dialog #(action-handler id action)
                                    (str action-title " container " containerName "?") action-title)
             (logs containerName log-content form-state)
             [:div.Swarmpit-form-toolbar
              (comp/grid
                {:container true :spacing 2}
                (comp/grid {:item true :xs 12}
                  (toolbar/toolbar "Container" containerName (actions container processing?)))
                (comp/grid {:item true :xs 12 :md (if resources? 5 12)} (details container))
                (when resources?
                  (comp/grid {:item true :xs 12 :md 7}
                    (comp/grid {:container true :spacing 2}
                      (resource-card "Ports" ports ports-metadata nil)
                      (resource-card "Networks" networks networks-metadata
                        #(when (:id %) (routes/path-for-frontend :network-info {:id (:id %)})))
                      (resource-card "Mounts" mounts mounts-metadata
                        #(when (:volumeName %) (routes/path-for-frontend :volume-info {:name (:volumeName %)})))))))]]))))))
