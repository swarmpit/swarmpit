(ns swarmpit.component.volume.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.docker.utils :as utils]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def driver-opts-headers ["Name" "Value"])

(def driver-opts-render-keys
  [[:name] [:value]])

(defn driver-opts-render-item
  [item]
  (val item))

(defn- volume-services-handler
  [volume-name]
  (ajax/get
    (routes/path-for-backend :volume-services {:name volume-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- volume-handler
  [volume-name]
  (ajax/get
    (routes/path-for-backend :volume {:name volume-name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:volume] response state/form-value-cursor))}))

(defn- delete-volume-handler
  [volume-name]
  (ajax/delete
    (routes/path-for-backend :volume-delete {:name volume-name})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :volume-list))
                   (message/info
                     (str "Volume " volume-name " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Volume removing failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-state)
      (volume-handler name)
      (volume-services-handler name))))

(rum/defc form-info < rum/static [{:keys [volume services]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/volumes
                 (:volumeName volume))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-volume-handler (:volumeName volume))
          :label      "Delete"}))]]
   [:div.form-layout
    [:div.form-layout-group
     (form/subsection "General settings")
     (form/item-stack (:stack volume))
     (form/item "NAME" (utils/trim-stack (:stack volume) (:volumeName volume)))
     (form/item "SCOPE" (:scope volume))
     (form/item "MOUNTPOINT" (:mountpoint volume))]
    [:div.form-layout-group.form-layout-group-border
     (form/subsection "Driver")
     (form/item "NAME" (:driver volume))
     (when (not-empty (:options volume))
       [:div
        (form/subsection "Volume driver options")
        (list/table driver-opts-headers
                    (:options volume)
                    driver-opts-render-item
                    driver-opts-render-keys
                    nil)])]
    (services/linked-services services)]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))