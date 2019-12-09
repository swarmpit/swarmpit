(ns swarmpit.component.service.form-logdriver
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :logdriver))

(def form-state-cursor (conj state/form-state-cursor :logdriver))

(def form-value-opts-cursor (conj form-value-cursor :opts))

(defn drivers-handler
  []
  (ajax/get
    (routes/path-for-backend :plugin-log)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:plugins] response form-state-cursor))}))

(defn- form-driver [value plugins]
  (comp/text-field
    {:fullWidth       true
     :key             "form-log-driver"
     :label           "Log Driver"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:name] (-> % .-target .-value) form-value-cursor)}
    (comp/menu-item
      {:key   "none"
       :value "none"} "none")
    (->> plugins
         (map #(comp/menu-item
                 {:key   %
                  :value %} %)))))

(defn- form-name [value index]
  (comp/text-field
    {:fullWidth       true
     :placeholder     "Name"
     :key             (str "form-logdriver-opt-name-" index)
     :defaultValue    value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :name (-> % .-target .-value) form-value-opts-cursor)}))

(defn- form-value [value index]
  (comp/text-field
    {:fullWidth       true
     :placeholder     "Value"
     :key             (str "form-logdriver-opt-value-" index)
     :defaultValue    value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :value (-> % .-target .-value) form-value-opts-cursor)}))

(def form-metadata
  [{:name      "Name"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-name value index))}
   {:name      "Value"
    :key       [:value]
    :render-fn (fn [value _ index] (form-value value index))}])

(defn- form-table
  [opts]
  (list/list
    form-metadata
    opts
    (fn [index] (state/remove-item index form-value-opts-cursor))))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-opts-cursor))

(rum/defc form < rum/reactive []
  (let [{:keys [name opts]} (state/react form-value-cursor)
        {:keys [plugins]} (state/react form-state-cursor)]
    (comp/grid
      {:container true}
      (comp/grid
        {:item true
         :xs   12} (form-driver name plugins))
      (comp/grid
        {:item true
         :xs   12}
        (form/subsection
          "Driver options"
          (comp/button
            {:color   "primary"
             :onClick add-item}
            (comp/svg icon/add-small-path) "Add option"))
        (when (not (empty? opts))
          (form-table opts))))))