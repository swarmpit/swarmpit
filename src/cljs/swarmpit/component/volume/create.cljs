(ns swarmpit.component.volume.create
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :volume :form])

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:volumeName] v cursor))})))

(defn- form-driver [value]
  (comp/form-comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v cursor))}
      (comp/menu-item
        {:key         "fdi1"
         :value       "local"
         :primaryText "local"}))))

(defn- create-volume-handler
  []
  (handler/post
    (routes/path-for-backend :volume-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :volume-info {:name (:volumeName response)}))
                   (message/info
                     (str "Volume " (:volumeName response) " has been created.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Volume creation failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:volumeName nil
                    :driver     "local"
                    :isValid    false} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin []
  (let [{:keys [volumeName
                driver
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/networks "New volume")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-volume-handler}))]]

     [:div.form-view
      [:div.form-view-group
       (comp/form
         {:onValid   #(state/update-value [:isValid] true cursor)
          :onInvalid #(state/update-value [:isValid] false cursor)}
         (form-name volumeName)
         (form-driver driver))]]]))