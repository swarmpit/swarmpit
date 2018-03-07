(ns swarmpit.component.volume.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- volume-plugin-handler
  []
  (ajax/get
    (routes/path-for-backend :plugin-volume)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:plugins] response state/form-state-cursor))}))

(defn- create-volume-handler
  []
  (ajax/post
    (routes/path-for-backend :volume-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :volume-info {:name (:volumeName response)})))
                   (message/info
                     (str "Volume " (:volumeName response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Volume creation failed. Reason: " (:error response))))}))

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:volumeName] v state/form-value-cursor))})))

(defn- form-driver [value plugins]
  (form/comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v state/form-value-cursor))}
      (->> plugins
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :plugins     []} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:volumeName nil
                    :driver     "local"} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value)
      (volume-plugin-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [volumeName driver]} (state/react state/form-value-cursor)
        {:keys [valid? processing? plugins]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/networks "New volume")]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Create"
          :disabled   (not valid?)
          :primary    true
          :onTouchTap create-volume-handler} processing?)]]
     [:div.form-edit
      (form/form
        {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
         :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
        (form-name volumeName)
        (form-driver driver plugins))]]))