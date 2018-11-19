(ns swarmpit.component.service.info.mounts
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def bind-render-metadata
  [{:name    "Container path"
    :primary true
    :key     [:containerPath]}
   {:name "Host path"
    :key  [:host]}
   {:name "Read only"
    :key  [:readOnly]}])

(def volume-render-metadata
  [{:name    "Container path"
    :primary true
    :key     [:containerPath]}
   {:name "Volume"
    :key  [:host]}
   {:name "Read only"
    :key  [:readOnly]}
   {:name "Driver"
    :key  [:volumeOptions :driver :name]}])

(defn onclick-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:host item)}))

(rum/defc form-bind < rum/static [bind]
  (when (not-empty bind)
    (list/table
      bind-render-metadata
      bind
      nil)))

(rum/defc form-volume < rum/static [volume]
  (when (not-empty volume)
    (list/table
      volume-render-metadata
      volume
      onclick-handler)))

(rum/defc form < rum/static [mounts]
  (let [bind (filter #(= "bind" (:type %)) mounts)
        volume (filter #(= "volume" (:type %)) mounts)]
    (comp/card
      {:className "Swarmpit-form-card"}
      (comp/card-header
        {:className "Swarmpit-form-card-header"
         :subheader (form/subheader "Mounts" icon/settings)})
      (comp/card-content
        {}
        (form-bind bind)
        (form-volume volume)))))