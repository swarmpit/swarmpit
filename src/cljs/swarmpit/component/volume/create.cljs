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

(def cursor [:form])

(defonce valid? (atom false))

(defonce volume-plugins (atom []))

(defn- volume-plugin-handler
  []
  (ajax/get
    (routes/path-for-backend :plugin-volume)
    {:on-success (fn [response]
                   (reset! volume-plugins response))}))

(defn- create-volume-handler
  []
  (ajax/post
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

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:volumeName] v cursor))})))

(defn- form-driver [value plugins]
  (form/comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v cursor))}
      (->> plugins
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- init-state
  []
  (state/set-value {:volumeName nil
                    :driver     "local"} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (volume-plugin-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [volumeName
                driver]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/networks "New volume")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not (rum/react valid?))
            :primary    true
            :onTouchTap create-volume-handler}))]]
     [:div.form-edit
       (form/form
         {:onValid   #(reset! valid? true)
          :onInvalid #(reset! valid? false)}
         (form-name volumeName)
         (form-driver driver (rum/react volume-plugins)))]]))