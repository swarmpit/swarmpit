(ns swarmpit.component.service.info
  (:require [swarmpit.material :as material]
            [swarmpit.router :as router]
            [swarmpit.component.message :as message]
            [rum.core :as rum]
            [ajax.core :refer [DELETE]]))

(enable-console-print!)

(def form-port-headers ["Container port" "Protocol" "Host port"])

(def table-el-height #js {:height "20px"})

(defn- container-port [item index]
  (material/table-row-column
    #js {:key   (str "containerPort" index)
         :style table-el-height}
    (get item "TargetPort")))

(defn- protocol [item index]
  (material/table-row-column
    #js {:key   (str "protocol" index)
         :style table-el-height}
    (get item "Protocol")))

(defn- host-port [item index]
  (material/table-row-column
    #js {:key   (str "hostPort" index)
         :style table-el-height}
    (get item "PublishedPort")))

(defn- ports [ports]
  (material/theme
    (material/table
      #js {:selectable false
           :style      #js {:width "30%"}}
      (material/table-header
        #js {:displaySelectAll  false
             :adjustForCheckbox false
             :style             #js {:border "none"}}
        (material/table-row
          #js {:displayBorder false
               :style         table-el-height}
          (map-indexed
            (fn [index header]
              (material/table-header-column
                #js {:key   (str "header" index)
                     :style table-el-height}
                header))
            form-port-headers)))
      (material/table-body
        #js {:showRowHover       false
             :displayRowCheckbox false}
        (map-indexed
          (fn [index item]
            (material/table-row
              #js {:key       (str "row" index)
                   :rowNumber index
                   :style     table-el-height}
              (container-port item index)
              (protocol item index)
              (host-port item index)))
          ports)))))

(rum/defc form < rum/static [item]
  (let [id (get item "ID")
        created-at (get item "CreatedAt")
        updated-at (get item "UpdatedAt")
        service-image (get-in item ["Spec" "TaskTemplate" "ContainerSpec" "Image"])
        service-name (get-in item ["Spec" "Name"])
        service-mode (first (keys (get-in item ["Spec" "Mode"])))
        service-ports (get-in item ["Spec" "EndpointSpec" "Ports"])]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:href    (str "/#/services/" id "/edit")
                :label   "Edit"
                :primary true
                :style   #js {:marginRight "12px"}}))
       (material/theme
         (material/raised-button
           #js {:onTouchTap (fn []
                              (DELETE (str "/services/" id)
                                      {:handler
                                       (fn [_]
                                         (router/dispatch! "/#/services")
                                         (message/mount!
                                           (str "Service " id " has been removed.")))
                                       :error-handler
                                       (fn [{:keys [status status-text]}]
                                         (message/mount!
                                           (str "Service " id " removing failed. Reason: " status-text)))}))
                :label      "Delete"}))]]
     [:div.form-view
      [:div.form-view-group
       (material/form-view-section "General settings")
       (material/form-view-row "ID" id)
       (material/form-view-row "CREATED" created-at)
       (material/form-view-row "LAST UPDATE" updated-at)
       (material/form-view-row "SERVICE IMAGE" service-image)
       (material/form-view-row "SERVICE NAME" service-name)
       (material/form-view-row "MODE" service-mode)]
      [:div.form-view-group
       (material/form-view-section "Ports")
       (ports service-ports)]
      [:div.form-view-group
       (material/form-view-section "Environment variables")]]]))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
