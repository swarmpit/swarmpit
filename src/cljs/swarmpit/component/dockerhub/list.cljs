(ns swarmpit.component.dockerhub.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [cljs.core :as core]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Username"
    :key     [:username]
    :primary true}
   {:name "Name"
    :key  [:name]}
   {:name "Company"
    :key  [:company]}
   {:name      "Public"
    :key       [:public]
    :render-fn (fn [value _] (if value "yes" "no"))}])

(defn- onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :dockerhub-user-info {:id (:_id item)})))

(defn- users-handler
  []
  (ajax/get
    (routes/path-for-backend :dockerhub-users)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(def form-actions
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch! (routes/path-for-frontend :dockerhub-user-create))} icon/add-circle)
    :name   "Add user"}])


(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (users-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (-> (core/filter #(= (:owner %) (storage/user)) items)
                           (list-util/filter (:query filter)))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (list/responsive
            render-metadata
            nil
            (sort-by :username filtered-items)
            onclick-handler)]]))))