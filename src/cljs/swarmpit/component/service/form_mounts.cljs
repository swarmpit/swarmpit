(ns swarmpit.component.service.form-mounts
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :mounts])

(defonce volumes (atom []))

(defn volumes-handler
  []
  (handler/get
    (routes/path-for-backend :volumes)
    {:on-success (fn [response]
                   (reset! volumes response))}))

(def headers [{:name  "Type"
               :width "15%"}
              {:name  "Container path"
               :width "35%"}
              {:name  "Host (path/volume)"
               :width "35%"}
              {:name  "Read only"
               :width "5%"}])

(def empty-info
  (comp/form-value "No mounts defined for the service."))

(defn- form-container [value index]
  (comp/form-list-textfield
    {:name     (str "form-container-path-text-" index)
     :key      (str "form-container-path-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :containerPath v cursor))}))

(defn- form-host-bind [value index]
  (comp/form-list-textfield
    {:name     (str "form-bind-path-text-" index)
     :key      (str "form-bind-path-text-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :host v cursor))}))

(defn- form-host-volume [value index volumes-list]
  (comp/form-list-selectfield
    {:name      (str "form-volume-select-" index)
     :key       (str "form-volume-select-" index)
     :value     value
     :autoWidth true
     :onChange  (fn [_ _ v]
                  (state/update-item index :host v cursor))}
    (->> volumes-list
         (map #(comp/menu-item
                 {:name        (str "form-volume-item-" (:volumeName %))
                  :key         (str "form-volume-item-" (:volumeName %))
                  :value       (:volumeName %)
                  :primaryText (:volumeName %)})))))

(defn- form-type [value index]
  (comp/form-list-selectfield
    {:name     (str "form-type-select-" index)
     :key      (str "form-type-select-" index)
     :value    value
     :onChange (fn [_ _ v]
                 (state/update-item index :type v cursor))}
    (comp/menu-item
      {:name        (str "form-type-bind-" index)
       :key         (str "form-type-bind-" index)
       :value       "bind"
       :primaryText "bind"})
    (comp/menu-item
      {:name        (str "form-type-volume-" index)
       :key         (str "form-type-volume-" index)
       :value       "volume"
       :primaryText "volume"})))

(defn- form-readonly [value index]
  (comp/checkbox
    {:name    (str "form-readonly-box-" index)
     :key     (str "form-readonly-box-" index)
     :checked value
     :onCheck (fn [_ v]
                (state/update-item index :readOnly v cursor))}))

(defn- render-mounts
  [item index data]
  (let [{:keys [containerPath
                host
                type
                readOnly]} item]
    [(form-type type index)
     (form-container containerPath index)
     (if (= "bind" type)
       (form-host-bind host index)
       (form-host-volume host index data))
     (form-readonly readOnly index)]))

(defn- form-table
  [mounts volumes-list]
  (comp/form-table headers
                   mounts
                   volumes-list
                   render-mounts
                   (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:type          "bind"
                   :containerPath ""
                   :host          ""
                   :readOnly      false} cursor))

(rum/defc form-create < rum/reactive []
  (let [volumes-list (rum/react volumes)
        mounts (state/react cursor)]
    [:div
     (comp/form-add-btn "Mount volume" add-item)
     (when (not (empty? mounts))
       (form-table mounts volumes-list))]))

(rum/defc form-update < rum/reactive []
  (let [volumes-list (rum/react volumes)
        mounts (state/react cursor)]
    (if (empty? mounts)
      empty-info
      (form-table mounts volumes-list))))