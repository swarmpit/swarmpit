(ns swarmpit.component.service.form-logdriver
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :logdriver))

(def form-value-opts-cursor (conj form-value-cursor :opts))

(defn- form-driver [value]
  (comp/text-field
    {:fullWidth       true
     :key             "form-log-driver"
     :label           "Driver"
     :select          true
     :value           value
     :variant         "outlined"
     :style           {:maxWidth "300px"}
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:name] (-> % .-target .-value) form-value-cursor)}
    (comp/menu-item
      {:key   "dr-none"
       :value "none"} "none")
    (comp/menu-item
      {:key   "dr-json-file"
       :value "json-file"} "json-file")
    (comp/menu-item
      {:key   "dr-journald"
       :value "journald"} "journald")))

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
  (rum/with-key
    (list/list
      form-metadata
      opts
      (fn [index] (state/remove-item index form-value-opts-cursor))) "form-ldriver-table"))

(defn- add-item
  []
  (state/add-item {:name  ""
                   :value ""} form-value-opts-cursor))

(rum/defc form < rum/reactive []
  (let [{:keys [name opts]} (state/react form-value-cursor)]
    (comp/grid
      {:container true
       :key       "seflogcg"}
      (comp/grid
        {:item true
         :key  "seflogcgid"
         :xs   12
         :sm   6} (form-driver name))
      (comp/grid
        {:item true
         :key  "seflogcgio"
         :xs   12}
        (form/subsection
          "Driver options"
          (comp/button
            {:color   "primary"
             :onClick add-item}
            (comp/svg
              {:key "seflogcgiobico"} icon/add-small-path) "Add option"))
        (when (not (empty? opts))
          (form-table opts))))))