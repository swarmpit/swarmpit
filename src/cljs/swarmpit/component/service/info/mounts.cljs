(ns swarmpit.component.service.info.mounts
  (:require [material.component :as comp]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def headers-bind ["Container path" "Host path" "Read only"])

(def headers-volume ["Container path" "Volume" "Read only"])

(def render-item-keys
  [[:containerPath] [:host] [:readOnly]])

(defn render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :readOnly (if value
                  "Yes"
                  "No")
      (val item))))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:host item)}))

(rum/defc form-bind < rum/static [bind]
  (when (not-empty bind)
    [:div
     (comp/form-subsection "Bind")
     (comp/list-table-auto headers-bind
                           bind
                           render-item
                           render-item-keys
                           nil)]))

(rum/defc form-volume < rum/static [volume]
  (when (not-empty volume)
    [:div
     (comp/form-subsection "Volume")
     (comp/list-table-auto headers-volume
                           volume
                           render-item
                           render-item-keys
                           onclick-handler)]))

(rum/defc form < rum/static [mounts]
  (when (not-empty mounts)
    (let [bind (filter #(= "bind" (:type %)) mounts)
          volume (filter #(= "volume" (:type %)) mounts)]
      [:div.form-service-view-group.form-service-group-border
       (comp/form-section "Mounts")
       (form-bind bind)
       (form-volume volume)])))