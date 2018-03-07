(ns swarmpit.component.stack.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def headers [{:name  "Name"
               :width "40%"}
              {:name  "Services"
               :width "10%"}
              {:name  "Networks"
               :width "10%"}
              {:name  "Volumes"
               :width "10%"}
              {:name  "Configs"
               :width "10%"}
              {:name  "Secrets"
               :width "10%"}
              {:name  ""
               :width "10%"}])

(def render-item-keys
  [[:stackName]
   [:stackStats :services]
   [:stackStats :networks]
   [:stackStats :volumes]
   [:stackStats :configs]
   [:stackStats :secrets]
   [:stackFile]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :stackFile (when value
                   (comp/svg icon/compose-18))
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :stack-info {:name (:stackName item)}))

(defn- format-response
  [response]
  (map #(hash-map
          :stackName (:stackName %)
          :stackFile (:stackFile %)
          :stackStats {:services (count (:services %))
                       :networks (count (:networks %))
                       :volumes  (count (:volumes %))
                       :configs  (count (:configs %))
                       :secrets  (count (:secrets %))}) response))

(defn- stack-handler
  []
  (ajax/get
    (routes/path-for-backend :stacks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (stack-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search stacks"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :stack-create)
            :label   "New stack"
            :primary true}))]]
     (list/table headers
                 (->> (list/filter items (:query filter))
                      (format-response)
                      (sort-by :stackName))
                 loading?
                 render-item
                 render-item-keys
                 onclick-handler)]))