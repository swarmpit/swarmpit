(ns swarmpit.component.service.create-image-public
  (:require [material.components :as comp]
            [material.component.list.basic :as list]
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
     :defaultValue    repository
     :margin          "normal"
     :variant         "outlined"
     :placeholder     "Find repository"
     :style           {:maxWidth "400px"}
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

(def render-list-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:description item))})

(rum/defc form-list < rum/reactive []
  (let [{:keys [searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)]
    (html
      [:div
       (when searching?
         (comp/linear-progress))
       (comp/list
         {:dense true}
         (map-indexed
           (fn [index item]
             (list/list-item
               render-list-metadata
               index
               item
               (last (:results repositories))
               #(onclick-handler (:name item)))) (:results repositories)))])))

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [{:keys [repository]} (state/react form-state-cursor)]
    [:div.Swarmpit-image-search
     (form-repository repository)]))