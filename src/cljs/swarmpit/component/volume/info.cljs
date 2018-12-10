(ns swarmpit.component.volume.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.list.basic :as list]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.docker.utils :as utils]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-driver-opts-render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(defn- section-general
  [{:keys [id stack volumeName driver mountpoint scope]}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     volumeName
       :classes   {:title "Swarmpit-card-header-responsive-title"}
       :className "Swarmpit-form-card-header"})
    (comp/card-content
      {}
      (html
        [:div
         [:span "Volume is mount at " [:b.volume-mountpoint mountpoint] "."]]))
    (comp/card-content
      {}
      (form/item-labels
        [(when driver
           (label/grey driver))]))
    (comp/card-actions
      {}
      (when stack
        (comp/button
          {:size  "small"
           :color "primary"
           :href  (routes/path-for-frontend :stack-info {:name stack})}
          "See stack")))
    (comp/divider)
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (comp/typography
        {:color "textSecondary"}
        (form/item-id id)))))

(defn- section-driver
  [{:keys [driver options]}]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Driver options"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (when (not-empty options)
        (list/list
          form-driver-opts-render-metadata
          options
          nil)))))

(defn- volume-services-handler
  [volume-name]
  (ajax/get
    (routes/path-for-backend :volume-services {:name volume-name})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- volume-handler
  [volume-name]
  (ajax/get
    (routes/path-for-backend :volume {:name volume-name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:volume] response state/form-value-cursor))}))

(defn- delete-volume-handler
  [volume-name]
  (ajax/delete
    (routes/path-for-backend :volume-delete {:name volume-name})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :volume-list))
                   (message/info
                     (str "Volume " volume-name " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Volume removing failed. " (:error response))))}))

(defn form-actions
  [{:keys [params]}]
  [{:onClick #(delete-volume-handler (:name params))
    :icon    (comp/svg icon/trash)
    :name    "Delete volume"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-state)
      (volume-handler name)
      (volume-services-handler name))))

(rum/defc form-info < rum/static [{:keys [volume services]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   40}
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (section-general volume))
          (when (not-empty (:options volume))
            (comp/grid
              {:item true
               :xs   12
               :sm   6}
              (section-driver volume)))
          (when (not-empty services)
            (comp/grid
              {:item true
               :xs   12}
              (services/linked services))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))