(ns swarmpit.component.container.info
  (:require [clojure.string :as str]
            [material.components :as comp]
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
                            (message/info "Container removed. Volumes have been preserved."))
                        (container-handler id))))})))

(defn- confirm-action
  [action title]
  (state/update-value [:action] action state/form-state-cursor)
  (state/update-value [:action-title] title state/form-state-cursor)
  (state/update-value [:open] true dialog/dialog-cursor))

(defn- actions
  [{:keys [id state managed]} processing?]
  (let [action (fn [value title]
                 {:name title :disabled processing?
                  :onClick #(confirm-action value title)})]
    (concat
      (when (and (storage/user?) (not managed))
        (case state
          ("created" "exited") [(action "start" "Start") (action "delete" "Delete")]
          "running" [(action "stop" "Stop") (action "restart" "Restart") (action "pause" "Pause")]
          "paused" [(action "unpause" "Resume")]
          "restarting" [(action "stop" "Stop")]
          "dead" [(action "delete" "Delete")]
          []))
      [{:name "Refresh" :variant "outlined" :onClick #(container-handler id)}])))

(defn- format-port
  [{:keys [containerPort hostIp hostPort]}]
  (if hostPort
    (str (if (str/includes? (or hostIp "") ":") (str "[" hostIp "]") hostIp)
         ":" hostPort " → " containerPort)
    containerPort))

(rum/defc details < rum/static [{:keys [id image state createdAt startedAt finishedAt exitCode
                                      error health restartPolicy command mounts networks ports] :as container}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (form/item-main "ID" id false)
    (form/item-main "Image" image)
    (form/item-main "State" (containers/container-state container))
    (when health (form/item-main "Health" health))
    (form/item-main "Managed by" (containers/owner container))
    (form/item-main "Created" (form/item-date createdAt))
    (when (not= "0001-01-01T00:00:00Z" startedAt)
      (form/item-main "Started" (form/item-date startedAt)))
    (when (contains? #{"exited" "dead"} state)
      (comp/box {}
        (form/item-main "Finished" (form/item-date finishedAt))
        (form/item-main "Exit code" exitCode)))
    (when (seq error) (form/item-main "Error" error))
    (form/item-main "Restart policy" (or (not-empty restartPolicy) "no"))
    (form/item-main "Command" (str/join " " command))
    (form/item-main "Ports" (str/join ", " (map format-port ports)))
    (form/item-main "Networks" (str/join ", " (map #(str (:networkName %) " " (:address %)) networks)))
    (form/item-main "Mounts"
      (html [:div
             (for [{:keys [source target type readOnly]} mounts]
               [:div {:key target}
                (str source " → " target " (" type (when readOnly ", read only") ")")])]))))

(rum/defc logs < rum/static [id content {:keys [logs-open? logs-loading? logs-error]}]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:title "Logs"
       :subheader "Last 200 lines of stdout and stderr"
       :action (comp/button
                 {:color "primary"
                  :onClick (fn []
                             (state/update-value [:logs-open?] (not logs-open?) state/form-state-cursor)
                             (when-not logs-open? (logs-handler id)))}
                 (if logs-open? "Hide logs" "Show logs"))})
    (when logs-open?
      (comp/card-content
        {}
        (when logs-error (form/error-message logs-error))
        (when (and logs-loading? (nil? content)) (comp/linear-progress {}))
        (html [:pre {:style {:maxHeight "480px" :overflow "auto" :whiteSpace "pre-wrap"
                            :overflowWrap "anywhere" :margin 0}}
               (or (not-empty content) (when-not logs-loading? "No logs available."))])))))

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
        {:keys [id containerName managed serviceId serviceName]} container]
    (progress/form
      loading?
      (when container
        (comp/mui
          (html
            [:div.Swarmpit-form
             (dialog/confirm-dialog
               #(action-handler id action)
               (str action-title " container " containerName "?"
                    (when (= "delete" action) " Its writable layer will be removed. Volumes will be preserved."))
               action-title)
             (comp/container
               {:maxWidth "md" :className "Swarmpit-container"}
               (comp/grid
                 {:container true :spacing 2}
                 (comp/grid {:item true :xs 12}
                   (toolbar/toolbar "Host container" containerName (actions container processing?)))
                 (when managed
                   (comp/grid {:item true :xs 12}
                     (form/item-info "This container is managed by Swarm. Use its service to make changes.")
                     (when serviceId
                       (comp/button {:color "primary" :href (routes/path-for-frontend :service-info {:id serviceId})}
                         (str "See service " serviceName)))))
                 (comp/grid {:item true :xs 12} (details container))
                 (comp/grid {:item true :xs 12} (logs id log-content form-state))))]))))))
