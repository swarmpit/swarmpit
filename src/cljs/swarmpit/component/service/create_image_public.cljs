(ns swarmpit.component.service.create-image-public
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def form-value-cursor (conj state/form-value-cursor :public))

(def form-state-cursor (conj state/form-state-cursor :public))

(defn- onclick-handler
  [repository]
  (dispatch!
    (routes/path-for-frontend :service-create-config
                              {}
                              {:repository repository})))

(defn- repository-handler
  [query page]
  (ajax/get
    (routes/path-for-backend :public-repositories)
    {:params     {:query query
                  :page  page}
     :state      [:public :searching?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response form-value-cursor))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Repositories fetching failed. " (:error response))))}))

(defn- form-repository [repository]
  (comp/text-field
    {:fullWidth       true
     :id              "repository"
     :label           "Repository"
     :value           repository
     :margin          "normal"
     :variant         "outlined"
     :placeholder     "Find repository"
     :InputLabelProps {:shrink true}
     :onChange        (fn [event]
                        (state/update-value [:repository] (-> event .-target .-value) form-state-cursor)
                        (repository-handler (-> event .-target .-value) 1))}))

(defn- init-form-state
  []
  (state/set-value {:searching? false
                    :repository ""} form-state-cursor))

(def mixin-init-form
  (mixin/init-tab
    (fn []
      (init-form-state))))

(rum/defc form-list < rum/static [searching? {:keys [results page limit total query]}]
  (html
    [:div
     (when searching?
       (comp/linear-progress))
     (comp/list
       {:dense true}
       (->> results
            (map (fn [item]
                   (comp/list-item
                     {:button         true
                      :onClick        #(onclick-handler (:name item))
                      :disableGutters true
                      :divider        true}
                     (comp/list-item-text
                       {:primary   (:name item)
                        :secondary (:description item)}))))))]))

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [{:keys [repository searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)]
    [:div.Swarmpit-form-context
     (form-repository repository)
     (form-list searching? repositories)]))