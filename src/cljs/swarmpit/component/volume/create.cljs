(ns swarmpit.component.volume.create
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.composite :as composite]
            [material.component.list.edit :as list]
            [material.component.form :as form]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def doc-volume-link "https://docs.docker.com/storage/volumes/")

(defn- volume-plugin-handler
  []
  (ajax/get
    (routes/path-for-backend :plugin-volume)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:plugins] response state/form-state-cursor))}))

(defn- create-volume-handler
  []
  (ajax/post
    (routes/path-for-backend :volumes)
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
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :defaultValue    value
     :helperText      "Specify volume name or leave empty for random"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:volumeName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-driver [value plugins]
  (comp/text-field
    {:fullWidth       true
     :id              "driver"
     :key             "driver"
     :label           "Volume driver"
     :helperText      "Driver to manage the Volume "
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:driver] (-> % .-target .-value) state/form-value-cursor)}
    (->> plugins
         (map #(comp/menu-item
                 {:key   %
                  :value %} %)))))

(def form-driver-opts-cursor (conj state/form-value-cursor :options))

(defn- form-driver-opt-name [value index]
  (comp/text-field
    {:placeholder  "Name"
     :variant      "outlined"
     :margin       "dense"
     :fullWidth    true
     :name         (str "form-driver-opt-name-" index)
     :key          (str "form-driver-opt-name-" index)
     :defaultValue value
     :onChange     #(state/update-item index :name (-> % .-target .-value) form-driver-opts-cursor)}))

(defn- form-driver-opt-value [value index]
  (comp/text-field
    {:placeholder  "Value"
     :variant      "outlined"
     :margin       "dense"
     :fullWidth    true
     :name         (str "form-driver-opt-value-" index)
     :key          (str "form-driver-opt-value-" index)
     :defaultValue value
     :onChange     #(state/update-item index :value (-> % .-target .-value) form-driver-opts-cursor)}))

(def form-driver-opts-render-metadata
  [{:name      "Name"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-driver-opt-name value index))}
   {:name      "Value"
    :key       [:value]
    :render-fn (fn [value _ index] (form-driver-opt-value value index))}])

(defn- add-driver-opt
  []
  (state/add-item {:name  ""
                   :value ""} form-driver-opts-cursor))

(defn- section-driver
  [{:keys [driver options]} plugins]
  (comp/box
    {}
    (form-driver driver plugins)
    (form/subsection
      "Driver options"
      (comp/button
        {:color   "primary"
         :onClick add-driver-opt}
        (comp/svg icon/add-small-path) "Add option"))
    (when (not (empty? options))
      (list/list
        form-driver-opts-render-metadata
        options
        (fn [index] (state/remove-item index form-driver-opts-cursor))))))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :plugins     []} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:volumeName ""
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
  (let [{:keys [volumeName driver options] :as item} (state/react state/form-value-cursor)
        {:keys [valid? processing? plugins]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/container
            {:maxWidth  "md"
             :className "Swarmpit-container"}
            (comp/card
              {:className "Swarmpit-form-card Swarmpit-fcard"}
              (comp/box
                {:className "Swarmpit-fcard-header"}
                (comp/typography
                  {:className "Swarmpit-fcard-header-title"
                   :variant   "h6"
                   :component "div"}
                  "Create volume"))
              (comp/card-content
                {:className "Swarmpit-fcard-content"}
                (comp/typography
                  {:variant   "body2"
                   :className "Swarmpit-fcard-message"}
                  "Persist data generated by and used by services")
                (form-name volumeName)
                (section-driver item plugins))
              (comp/card-actions
                {:className "Swarmpit-fcard-actions"}
                (composite/progress-button
                  "Create"
                  #(create-volume-handler)
                  processing?))))]]))))