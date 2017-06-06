(ns swarmpit.component.registry.wizard
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(def cursor [:page :registry :wizard])

(defn- version
  [name items]
  (->> items
       (filter #(= name (:name %)))
       (first)
       :version))

(defn- form-registry [value items]
  (comp/form-comp
    "REGISTRY"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:name] v cursor))}
      (->> items
           (map #(comp/menu-item
                   {:key         (:name %)
                    :value       (:name %)
                    :primaryText (:name %)}))))))

(rum/defc registry-wizard < rum/reactive [items]
  (let [{:keys [name]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/wizard "Step 1")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label   "NEXT"
            :href    (str "/#/services/create/wizard/" (version name items) "/registries/" name "/repo")
            :primary true}))]]
     [:div.form-edit
      (form-registry name items)]]))

(defn- init-state
  [registries]
  (state/set-value (select-keys (first registries) [:name]) cursor))

(defn mount!
  [registries]
  (init-state registries)
  (rum/mount (registry-wizard registries) (.getElementById js/document "content")))
