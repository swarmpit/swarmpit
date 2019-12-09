(ns swarmpit.component.service.info.mounts
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-bind-metadata
  {:table {:summary [{:name      "Container path"
                      :render-fn (fn [item] (:containerPath item))}
                     {:name      "Host path"
                      :render-fn (fn [item] (:host item))}
                     {:name      "Read only"
                      :render-fn (fn [item] (when (true? (:readOnly item))
                                              "yes"))}]}
   :list  {:primary   (fn [item] (:containerPath item))
           :secondary (fn [item] (:host item))}})

(def render-volume-metadata
  {:table {:summary [{:name      "Container path"
                      :render-fn (fn [item] (:containerPath item))}
                     {:name      "Volume"
                      :render-fn (fn [item] (:host item))}
                     {:name      "Read only"
                      :render-fn (fn [item] (:readOnly item))}
                     {:name      "Driver"
                      :render-fn (fn [item] (get-in item [:volumeOptions :driver :name]))}]}
   :list  {:primary   (fn [item] (:containerPath item))
           :secondary (fn [item] (:host item))}})

(defn onclick-volume-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:host item)}))

(rum/defc form-bind < rum/static [bind]
  (when (not-empty bind)
    (list/responsive
      render-bind-metadata
      bind
      nil)))

(rum/defc form-named-volume < rum/static [volume]
  (when (not-empty volume)
    (list/responsive
      render-volume-metadata
      volume
      onclick-volume-handler)))

(rum/defc form-anonymous-volume < rum/static [volume]
  (when (not-empty volume)
    (list/responsive
      render-volume-metadata
      volume
      nil)))

(rum/defc form < rum/static [mounts service-id immutable?]
  (let [bind (filter #(= "bind" (:type %)) mounts)
        named-volume (filter #(and (= "volume" (:type %)) (some? (:host %))) mounts)
        anonymous-volume (filter #(and (= "volume" (:type %)) (nil? (:host %))) mounts)]
    (comp/card
      {:className "Swarmpit-card"}
      (comp/card-header
        {:className "Swarmpit-table-card-header"
         :title     (comp/typography {:variant "h6"} "Mounts")
         :action    (comp/icon-button
                      {:aria-label "Edit"
                       :disabled   immutable?
                       :href       (routes/path-for-frontend
                                     :service-edit
                                     {:id service-id}
                                     {:section 2})}
                      (comp/svg icon/edit-path))})

      (if (empty? mounts)
        (comp/card-content
          {}
          (form/item-info "No mounts defined for the service."))
        (comp/card-content
          {:className "Swarmpit-table-card-content"}
          (form-bind bind)
          (form-named-volume named-volume)
          (form-anonymous-volume anonymous-volume))))))