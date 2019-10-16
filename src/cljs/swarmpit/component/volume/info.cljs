(ns swarmpit.component.volume.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.list.basic :as list]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

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
    (routes/path-for-backend :volume {:name volume-name})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :volume-list))
                   (message/info
                     (str "Volume " volume-name " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Volume removing failed. " (:error response))))}))

(def form-driver-opts-render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form-general < rum/static [{:keys [id stack volumeName driver mountpoint scope]}
                                     services]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     volumeName
       :classes   {:title "Swarmpit-card-header-responsive-title"}
       :className "Swarmpit-form-card-header"
       :action    (comp/tooltip
                    {:title "Delete volume"}
                    (comp/icon-button
                      {:aria-label "Delete"
                       :onClick    #(state/update-value [:open] true dialog/dialog-cursor)}
                      (comp/svg icon/trash-path)))})
    (comp/card-content
      {}
      (html
        [:div.Swarmpit-volume-mount
         [:span "Volume is mounted at:"]
         [:span.Swarmpit-volume-mountpoint [:b mountpoint]]]))
    (comp/card-content
      {}
      (form/item-labels
        [(when driver
           (label/grey driver))]))
    (comp/card-actions
      {}
      (when (and stack (not-empty services))
        (comp/button
          {:size  "small"
           :color "primary"
           :href  (routes/path-for-frontend :stack-info {:name stack})}
          "See stack")))
    (comp/divider
      {})
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (form/item-id id))))

(rum/defc form-driver < rum/static [{:keys [driver options]}]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header Swarmpit-card-header-responsive-title"
       :title     (comp/typography {:variant "h6"} "Driver")})
    (comp/card-content {} driver)
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (when (not-empty options)
        [(comp/divider {})
         (list/list
           form-driver-opts-render-metadata
           options
           nil)]))))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-state)
      (volume-handler name)
      (volume-services-handler name))))

(defn form-general-grid [volume services]
  (comp/grid
    {:item true
     :xs   12}
    (form-general volume services)))

(defn form-driver-grid [volume]
  (comp/grid
    {:item true
     :xs   12}
    (form-driver volume)))

(defn form-services-grid [services]
  (comp/grid
    {:item true
     :xs   12}
    (services/linked services)))

(rum/defc form-info < rum/static [{:keys [volume services]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-volume-handler (:volumeName volume))
         "Delete volume?"
         "Delete")
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :sm   6
               :md   4}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-general-grid volume services)
                (form-driver-grid volume)))
            (comp/grid
              {:item true
               :sm   6
               :md   8}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-services-grid services)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid volume services)
            (form-services-grid services)
            (form-driver-grid volume)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
