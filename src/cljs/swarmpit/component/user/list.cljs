(ns swarmpit.component.user.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

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

(defn onclick-handler
  [item]
  (routes/path-for-frontend :user-info {:id (:_id item)}))

(defn- users-handler
  []
  (ajax/get
    (routes/path-for-backend :users)
    {:state      [:loading?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (users-handler))))

(def toolbar-render-metadata
  [{:name     "New user"
    :onClick  #(dispatch! (routes/path-for-frontend :user-create))
    :primary  true
    :icon     (icon/add-circle-out)
    :icon-alt (icon/add)}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [query]} (state/react state/search-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items query)
                            (sort-by :username))]
    (progress/form
      loading?
      (common/list "Users"
                   items
                   filtered-items
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))
