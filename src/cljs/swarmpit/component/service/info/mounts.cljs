(ns swarmpit.component.service.info.mounts
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as list]
            [rum.core :as rum]))

(enable-console-print!)

(def headers-bind ["Container path" "Host path" "Read only"])

(def headers-volume ["Container path" "Volume" "Read only" "Driver"])

(def render-item-keys
  [[:containerPath] [:host] [:readOnly] [:volumeOptions :driver :name]])

(defn render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :readOnly (if value
                  "Yes"
                  "No")
      (val item))))

(rum/defc form-bind < rum/static [bind]
  (when (not-empty bind)
    [:div
     (form/subsection "Bind")
     (list/table headers-bind
                 bind
                 render-item
                 render-item-keys
                 nil)]))

(rum/defc form-volume < rum/static [volume]
  (when (not-empty volume)
    [:div
     (form/subsection "Volume")
     (list/table headers-volume
                 volume
                 render-item
                 render-item-keys
                 nil)]))

(rum/defc form < rum/static [mounts]
  (when (not-empty mounts)
    (let [bind (filter #(= "bind" (:type %)) mounts)
          volume (filter #(= "volume" (:type %)) mounts)]
      [:div.form-layout-group.form-layout-group-border
       (form/section "Mounts")
       (form-bind bind)
       (form-volume volume)])))