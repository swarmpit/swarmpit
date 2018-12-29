(ns swarmpit.component.secret.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.util :as list-util]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.common :as common]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:secretName item))}
                     {:name      "Created"
                      :render-fn (fn [item] (time/humanize (:createdAt item)))}]}
   :list  {:primary   (fn [item] (:secretName item))
           :secondary (fn [item] (time/humanize (:createdAt item)))}})

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :secret-info {:id (:secretName item)})))

(defn- secrets-handler
  []
  (ajax/get
    (routes/path-for-backend :secrets)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (secrets-handler))))

(def toolbar-render-metadata
  {:actions [{:name     "New secret"
              :onClick  #(dispatch! (routes/path-for-frontend :secret-create))
              :icon     icon/add-circle-out
              :icon-alt icon/add}]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)]
    (progress/form
      loading?
      (common/list "Secrets"
                   items
                   (->> (list-util/filter items (:query filter))
                        (sort-by :secretName))
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))
