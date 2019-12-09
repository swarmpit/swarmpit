(ns swarmpit.component.service.form-mounts
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :mounts))

(def form-state-cursor (conj state/form-state-cursor :mounts))

(defn- normalize-volume
  "Associate volumes with optional data (driver, labels)"
  [volume]
  (let [volume-data (first
                      (filter #(= (:host volume)
                                  (:volumeName %)) (:volumes (state/get-value form-state-cursor))))]
    (-> volume
        (assoc-in [:volumeOptions :labels] (:labels volume-data))
        (assoc-in [:volumeOptions :driver :name] (:driver volume-data))
        (assoc-in [:volumeOptions :driver :options] (:options volume-data)))))

(defn normalize
  "Associate mounts with optional data required for consistency"
  []
  (->> (state/get-value form-value-cursor)
       (map (fn [mount] (if (= "volume" (:type mount))
                          (normalize-volume mount)
                          mount)))
       (into [])))

(defn- form-container [value index]
  (comp/text-field
    {:label           "Container path"
     :fullWidth       true
     :key             (str "form-mount-container-" index)
     :variant         "outlined"
     :margin          "dense"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :containerPath (-> % .-target .-value) form-value-cursor)}))

(defn- form-host-bind [value index]
  (comp/text-field
    {:label           "Host path"
     :fullWidth       true
     :key             (str "form-mount-bind-" index)
     :variant         "outlined"
     :margin          "dense"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :host (-> % .-target .-value) form-value-cursor)}))

(defn- form-host-volume [value index volumes-list]
  (comp/text-field
    {:fullWidth       true
     :label           "Volume"
     :key             (str "form-mount-volume-" index)
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-item index :host (-> % .-target .-value) form-value-cursor)}
    (->> volumes-list
         (map #(comp/menu-item
                 {:key   (str "form-mount-volume-" (:volumeName %))
                  :value (:volumeName %)} (:volumeName %))))))

(defn- form-type [value index]
  (comp/text-field
    {:fullWidth       true
     :key             (str "form-mount-type-" index)
     :label           "Type"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-item index :type (-> % .-target .-value) form-value-cursor)}
    (comp/menu-item
      {:key   "bind"
       :value "bind"} "bind")
    (comp/menu-item
      {:key   "volume"
       :value "volume"} "volume")))

(defn- form-readonly [value index]
  (comp/checkbox
    {:key      (str "form-mount-readonly-" index)
     :checked  value
     :color    "primary"
     :onChange #(state/update-item index :readOnly (-> % .-target .-checked) form-value-cursor)}))

(defn- form-mounts-metadata [volume-list]
  [{:name      "Type"
    :primary   true
    :key       [:type]
    :render-fn (fn [value _ index] (form-type value index))}
   {:name      "Container path"
    :key       [:containerPath]
    :render-fn (fn [value _ index] (form-container value index))}
   {:name      "Host"
    :key       [:host]
    :render-fn (fn [value item index]
                 (if (= "bind" (:type item))
                   (form-host-bind value index)
                   (form-host-volume value index volume-list)))}
   {:name      "Read only"
    :key       [:readOnly]
    :render-fn (fn [value _ index]
                 (comp/form-control
                   {:component "fieldset"
                    :fullWidth true}
                   (comp/form-group
                     {}
                     (comp/form-control-label
                       {:control (form-readonly value index)
                        :label   "Read Only"}))))}])

(defn- form-table
  [mounts volume-list]
  (comp/list
    {:dense true}
    (map-indexed
      (fn [index item]
        (list/list-item-small
          (form-mounts-metadata volume-list)
          index
          item
          (fn [index] (state/remove-item index form-value-cursor)))) mounts)))

(defn add-item
  []
  (state/add-item {:type          "bind"
                   :containerPath ""
                   :host          ""
                   :readOnly      false} form-value-cursor))

(defn volumes-handler
  []
  (ajax/get
    (routes/path-for-backend :volumes)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:volumes] response form-state-cursor))}))

(rum/defc form < rum/reactive []
  (let [{:keys [volumes]} (state/react form-state-cursor)
        mounts (state/react form-value-cursor)]
    (if (empty? mounts)
      (form/item-info "No mounts defined for the service.")
      (form-table mounts volumes))))
