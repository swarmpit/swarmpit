(ns swarmpit.component.volume.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list-table-form :as list]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-driver-opts-cursor (conj state/form-value-cursor :options))

(def form-driver-opts-headers
  [{:name  "Name"
    :width "35%"}
   {:name  "Value"
    :width "35%"}])

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
                     (str "Volume creation failed. " (:error response))))}))

(defn- form-name [value]
  (form/comp
    "VOLUME NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:volumeName] v state/form-value-cursor))})))

(defn- form-driver [value plugins]
  (form/comp
    "NAME"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v state/form-value-cursor))}
      (->> plugins
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- form-driver-opt-name [value index]
  (list/textfield
    {:name     (str "form-driver-opt-name-" index)
     :key      (str "form-driver-opt-name-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :name v form-driver-opts-cursor))}))

(defn- form-driver-opt-value [value index]
  (list/textfield
    {:name     (str "form-driver-opt-value-" index)
     :key      (str "form-driver-opt-value-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v form-driver-opts-cursor))}))

(defn- form-driver-render-opts
  [item index]
  (let [{:keys [name value]} item]
    [(form-driver-opt-name name index)
     (form-driver-opt-value value index)]))

(defn- form-driver-opts-table
  [opts]
  (list/table-raw form-driver-opts-headers
                  opts
                  nil
                  form-driver-render-opts
                  (fn [index] (state/remove-item index form-driver-opts-cursor))))

(defn- add-driver-opt
  []
  (state/add-item {:name  ""
                   :value ""} form-driver-opts-cursor))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :plugins     []} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:volumeName nil
                    :driver     "local"
                    :options    []} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value)
      (volume-plugin-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [volumeName driver options]} (state/react state/form-value-cursor)
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
     [:div.form-layout
      [:div.form-layout-group
       (form/form
         {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
          :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
         (form-name volumeName))]
      [:div.form-layout-group.form-layout-group-border
       (form/section "Driver")
       (form/form
         {}
         (form-driver driver plugins)
         (html (form/subsection-add "Add volume driver option" add-driver-opt))
         (when (not (empty? options))
           (form-driver-opts-table options)))]]]))