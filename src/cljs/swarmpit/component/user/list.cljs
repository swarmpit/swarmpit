(ns swarmpit.component.user.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Username"
                      :render-fn (fn [item] (:username item))}
                     {:name      "Email"
                      :render-fn (fn [item] (:email item))}
                     {:name      "Is Admin"
                      :render-fn (fn [item] (if (:role item) "yes" "no"))}]}
   :list  {:primary   (fn [item] (:username item))
           :secondary (fn [item] (:email item))}})

(defn- onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :user-info {:id (:_id item)})))

(defn- users-handler
  []
  (ajax/get
    (routes/path-for-backend :users)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(def form-actions
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch! (routes/path-for-frontend :user-create))} icon/add-circle)
    :name   "New user"}])

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
        filtered-items (list-util/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/card
            {:className "Swarmpit-card"}
            (comp/card-header
              {:className "Swarmpit-table-card-header"
               :title     "Overview"})
            (comp/card-content
              {:className "Swarmpit-table-card-content"}
              (list/responsive
                render-metadata
                (sort-by :username filtered-items)
                onclick-handler)))]]))))