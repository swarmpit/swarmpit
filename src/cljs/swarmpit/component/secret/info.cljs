(ns swarmpit.component.secret.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.inflect :as inflect]
            [rum.core :as rum]))

(enable-console-print!)

(defn- secret-services-handler
  [secret-id]
  (ajax/get
    (routes/path-for-backend :secret-services {:id secret-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- secret-handler
  [secret-id]
  (ajax/get
    (routes/path-for-backend :secret {:id secret-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:secret] response state/form-value-cursor))}))

(defn- delete-secret-handler
  [secret-id]
  (ajax/delete
    (routes/path-for-backend :secret-delete {:id secret-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :secret-list))
                   (message/info
                     (str "Secret " secret-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Secret removing failed. " (:error response))))}))

(rum/defc form-general < rum/static [secret services]
  (comp/card
    {:className "Swarmpit-form-card"
     :key       "sgc"}
    (comp/card-header
      {:title     (:secretName secret)
       :className "Swarmpit-form-card-header"
       :key       "sgch"
       :action    (comp/tooltip
                    {:title "Delete secret"
                     :key   "sgchadt"}
                    (comp/icon-button
                      {:aria-label "Delete"
                       :onClick    #(delete-secret-handler (:id secret))}
                      (comp/svg icon/trash-path)))})
    (comp/card-content
      {:key "sgcci"}
      (html
        (if (empty? services)
          [:span "Secret is not used by any service"]
          [:span "Secret is used within " [:b (count services)] " " (inflect/pluralize-noun (count services) "service")])))
    (comp/divider
      {:key "sgd"})
    (comp/card-content
      {:style {:paddingBottom "16px"}
       :key   "sgccf"}
      (form/item-date (:createdAt secret) (:updatedAt secret))
      (form/item-id (:id secret)))))

(defn form-actions
  [{:keys [params]}]
  [{:onClick #(delete-secret-handler (:id params))
    :icon    (comp/svg icon/trash-path)
    :name    "Delete secret"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (secret-handler id)
      (secret-services-handler id))))

(defn form-general-grid [secret services]
  (comp/grid
    {:item true
     :key  "sgg"
     :xs   12}
    (rum/with-key
      (form-general secret services) "sggfg")))

(defn form-services-grid [services]
  (comp/grid
    {:item true
     :key  "ssg"
     :xs   12}
    (services/linked services)))

(rum/defc form-info < rum/static [{:keys [secret services]}]
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
                (form-general-grid secret services)))
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
            (form-general-grid secret services)
            (form-services-grid services)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))