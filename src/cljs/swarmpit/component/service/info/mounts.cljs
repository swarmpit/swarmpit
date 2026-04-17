(ns swarmpit.component.service.info.mounts
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.storage :as storage]))

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

(defn- driver-cell [item]
  (if (= "tmpfs" (:type item))
    "tmpfs"
    (get-in item [:volumeOptions :driver :name])))

(def render-volume-metadata
  {:table {:summary [{:name      "Container path"
                      :render-fn (fn [item] (:containerPath item))}
                     {:name      "Volume"
                      :render-fn (fn [item] (:host item))}
                     {:name      "Read only"
                      :render-fn (fn [item] (:readOnly item))}
                     {:name      "Driver"
                      :render-fn driver-cell}]}
   :list  {:primary   (fn [item] (:containerPath item))
           :secondary (fn [item] (:host item))}})

(def render-anonymous-volume-metadata
  {:table {:summary [{:name      "Container path"
                      :render-fn (fn [item] (:containerPath item))}
                     {:name      "Read only"
                      :render-fn (fn [item] (when (true? (:readOnly item))
                                              "yes"))}
                     {:name      "Driver"
                      :render-fn driver-cell}]}
   :list  {:primary   (fn [item] (:containerPath item))
           :secondary driver-cell}})

(defn onclick-volume-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:host item)}))

(defn- edit-action [service-id immutable?]
  (when (storage/user?)
    (comp/icon-button
      {:aria-label "Edit"
       :disabled   immutable?
       :href       (routes/path-for-frontend
                     :service-edit
                     {:id service-id}
                     {:section 2})}
      (comp/svg icon/edit-path))))

(rum/defc mount-card < rum/static
  [{:keys [title items metadata onclick service-id immutable?]}]
  (when (not-empty items)
    (comp/card
      {:className "Swarmpit-card"}
      (comp/card-header
        {:className "Swarmpit-table-card-header"
         :title     (comp/typography {:variant "h6"} title)
         :action    (edit-action service-id immutable?)})
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/responsive metadata items onclick)))))

(rum/defc form < rum/static [mounts service-id immutable?]
  (let [bind (filter #(= "bind" (:type %)) mounts)
        named-volume (filter #(and (= "volume" (:type %)) (some? (:host %))) mounts)
        anonymous-volume (filter #(or (and (= "volume" (:type %)) (nil? (:host %)))
                                      (= "tmpfs" (:type %))) mounts)]
    (if (empty? mounts)
      (comp/card
        {:className "Swarmpit-card"}
        (comp/card-header
          {:className "Swarmpit-table-card-header"
           :title     (comp/typography {:variant "h6"} "Mounts")
           :action    (edit-action service-id immutable?)})
        (comp/card-content
          {}
          (form/item-info "No mounts defined for the service.")))
      (html
        [:div.Swarmpit-mounts
         (mount-card {:title      "Binds"
                      :items      bind
                      :metadata   render-bind-metadata
                      :onclick    nil
                      :service-id service-id
                      :immutable? immutable?})
         (mount-card {:title      "Volumes"
                      :items      named-volume
                      :metadata   render-volume-metadata
                      :onclick    onclick-volume-handler
                      :service-id service-id
                      :immutable? immutable?})
         (mount-card {:title      "Anonymous volumes"
                      :items      anonymous-volume
                      :metadata   render-anonymous-volume-metadata
                      :onclick    nil
                      :service-id service-id
                      :immutable? immutable?})]))))