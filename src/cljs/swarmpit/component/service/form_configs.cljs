(ns swarmpit.component.service.form-configs
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :configs))

(def form-state-cursor (conj state/form-state-cursor :configs))

(def undefined-info
  (html
    [:span.Swarmpit-message
     [:span "No configs found. Create new "
      [:a {:href (routes/path-for-frontend :config-create)} "config."]]]))

(defn- form-config [value index configs-list]
  (comp/text-field
    {:fullWidth       true
     :label           "Name"
     :key             (str "form-config-name-" index)
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-item index :configName (-> % .-target .-value) form-value-cursor)}
    (->> configs-list
         (map #(comp/menu-item
                 {:key   (str "form-config-item-" (:configName %))
                  :value (:configName %)} (:configName %))))))

(defn- form-config-target [value name index]
  (comp/text-field
    {:label           "Target"
     :fullWidth       true
     :key             (str "form-config-target-" index)
     :placeholder     (when (str/blank? value) name)
     :variant         "outlined"
     :margin          "dense"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :configTarget (-> % .-target .-value) form-value-cursor)}))

(defn- form-configs-metadata [configs-list]
  [{:name      "Name"
    :primary   true
    :key       [:configName]
    :render-fn (fn [value _ index] (form-config value index configs-list))}
   {:name      "Target"
    :key       [:configTarget]
    :render-fn (fn [value item index] (form-config-target value (:configName item) index))}])

(defn- form-table
  [configs configs-list]
  (list/list
    (form-configs-metadata configs-list)
    configs
    (fn [index] (state/remove-item index form-value-cursor))))

(defn add-item
  []
  (state/add-item {:configName   ""
                   :configTarget ""} form-value-cursor))

(defn configs-handler
  []
  (ajax/get
    (routes/path-for-backend :configs)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:list] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        configs (state/react form-value-cursor)]
    (if (empty? configs)
      (form/item-info "No configs defined for the service.")
      (if (empty? list)
        undefined-info
        (form-table configs list)))))
