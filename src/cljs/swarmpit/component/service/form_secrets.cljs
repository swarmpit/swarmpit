(ns swarmpit.component.service.form-secrets
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [swarmpit.ajax :as ajax]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :secrets))

(def form-state-cursor (conj state/form-state-cursor :secrets))

(def undefined-info
  (html
    [:span.Swarmpit-message
     [:span "No secrets found. Create new "
      [:a {:href (routes/path-for-frontend :secret-create)} "secret."]]]))

(defn- form-secret [value index secrets-list]
  (comp/text-field
    {:fullWidth       true
     :label           "Name"
     :key             (str "form-secret-name-" index)
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-item index :secretName (-> % .-target .-value) form-value-cursor)}
    (->> secrets-list
         (map #(comp/menu-item
                 {:key   (str "form-secret-item-" (:secretName %))
                  :value (:secretName %)} (:secretName %))))))

(defn- form-secret-target [value name index]
  (comp/text-field
    {:label           "Target"
     :fullWidth       true
     :key             (str "form-secret-target-" index)
     :placeholder     (when (str/blank? value) name)
     :variant         "outlined"
     :margin          "dense"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :secretTarget (-> % .-target .-value) form-value-cursor)}))

(defn- form-secrets-metadata [secrets-list]
  [{:name      "Name"
    :primary   true
    :key       [:secretName]
    :render-fn (fn [value _ index] (form-secret value index secrets-list))}
   {:name      "Target"
    :key       [:secretTarget]
    :render-fn (fn [value item index] (form-secret-target value (:secretName item) index))}])

(defn- form-table
  [secrets secrets-list]
  (list/list
    (form-secrets-metadata secrets-list)
    secrets
    (fn [index] (state/remove-item index form-value-cursor))))

(defn add-item
  []
  (state/add-item {:secretName   ""
                   :secretTarget ""} form-value-cursor))

(defn secrets-handler
  []
  (ajax/get
    (routes/path-for-backend :secrets)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:list] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        secrets (state/react form-value-cursor)]
    (if (empty? secrets)
      (form/item-info "No secrets defined for the service.")
      (if (empty? list)
        undefined-info
        (form-table secrets list)))))
