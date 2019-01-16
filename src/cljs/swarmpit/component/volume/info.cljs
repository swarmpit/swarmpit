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

(def form-driver-opts-render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form-general < rum/static [{:keys [id stack volumeName driver mountpoint scope]}
                                     services]
  (comp/card
    {:className "Swarmpit-form-card"
     :key       "vgc"}
    (comp/card-header
      {:title     volumeName
       :classes   {:title "Swarmpit-card-header-responsive-title"}
       :key       "vgch"
       :className "Swarmpit-form-card-header"
       :action    (comp/tooltip
                    {:title "Delete volume"
                     :key   "vgchadt"}
                    (comp/icon-button
                      {:aria-label "Delete"
                       :onClick    #(delete-volume-handler volumeName)}
                      (comp/svg icon/trash-path)))})
    (comp/card-content
      {:key "vgcc"}
      (html
        [:div {:key "vgccd"}
         [:span "Volume is mount at " [:b.volume-mountpoint mountpoint] "."]]))
    (comp/card-content
      {:key "vgccl"}
      (form/item-labels
        [(when driver
           (label/grey driver))]))
    (comp/card-actions
      {:key "vgca"}
      (when (and stack (not-empty services))
        (comp/button
          {:size  "small"
           :color "primary"
           :href  (routes/path-for-frontend :stack-info {:name stack})}
          "See stack")))
    (comp/divider
      {:key "vgd"})
    (comp/card-content
      {:style {:paddingBottom "16px"}
       :key   "vgccf"}
      (form/item-id id))))

(rum/defc form-driver < rum/static [{:keys [driver options]}]
  (comp/card
    {:className "Swarmpit-card"
     :key       "vdc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "vdch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "driver-title"} "Driver")})
    (comp/card-content
      {:className "Swarmpit-table-card-content"
       ::key      "vdcc"}
      (when (not-empty options)
        (rum/with-key
          (list/list
            form-driver-opts-render-metadata
            options
            nil) "vdccrl")))))

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
     :key  "vgg"
     :xs   12}
    (rum/with-key
      (form-general volume services) "nggfg")))

(defn form-driver-grid [volume]
  (comp/grid
    {:item true
     :key  "vdg"
     :xs   12}
    (rum/with-key
      (form-driver volume) "ndgfg")))

(defn form-services-grid [services]
  (comp/grid
    {:item true
     :key  "vsg"
     :xs   12}
    (services/linked services)))

(rum/defc form-info < rum/static [{:keys [volume services]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :key  "slg"
               :sm   6
               :md   6
               :lg   4}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-general-grid volume services)
                (form-driver-grid (:options volume))))
            (comp/grid
              {:item true
               :key  "srg"
               :sm   6
               :md   6
               :lg   8}
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
            (form-driver-grid (:options volume))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))