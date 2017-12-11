(ns swarmpit.component.volume.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [swarmpit.docker-utils :as utils]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- volume-handler
  [volume-name]
  (handler/get
    (routes/path-for-backend :volume {:name volume-name})
    {:on-success (fn [response]
                   (state/set-value response cursor))}))

(defn- delete-volume-handler
  [volume-name]
  (handler/delete
    (routes/path-for-backend :volume-delete {:name volume-name})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :volume-list))
                   (message/info
                     (str "Volume " volume-name " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Volume removing failed. Reason: " (:error response))))}))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (volume-handler name))))

(rum/defc form-info < rum/static [volume]
  (let [stack (:stack volume)]
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
     [:div.form-view
      [:div.form-view-group
       (if (some? stack)
         (form/item "STACK" stack))
       (form/item "NAME" (utils/trim-stack stack (:volumeName volume)))
       (form/item "DRIVER" (:driver volume))
       (form/item "SCOPE" (:scope volume))
       (form/item "MOUNTPOINT" (:mountpoint volume))]]]))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [volume (state/react cursor)]
    (progress/form
      (nil? volume)
      (form-info volume))))