(ns swarmpit.component.service.create-image-public
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(def form-value-cursor (conj state/form-value-cursor :public))

(def form-state-cursor (conj state/form-state-cursor :public))

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- onclick-handler
  [index repositories]
  (let [repository (fn [index] (:name (nth repositories index)))]
    (dispatch!
      (routes/path-for-frontend :service-create-config
                                {}
                                {:repository (repository index)}))))

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
  (form/comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Find repository"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v form-state-cursor)
                   (repository-handler v 1))})))

(defn- init-form-state
  []
  (state/set-value {:searching? false
                    :repository ""} form-state-cursor))

(def mixin-init-form
  (mixin/init-tab
    (fn []
      (init-form-state))))

(rum/defc form-list < rum/static [searching? {:keys [results page limit total query]}]
  [:div.form-edit-loader
   (if searching?
     (progress/loading)
     (progress/loaded))
   (comp/mui
     (comp/table
       {:key         "tbl"
        :selectable  false
        :onCellClick (fn [i] (onclick-handler i results))}
       (list/table-header headers)
       (list/table-body headers
                        results
                        render-item
                        [[:name] [:description]])
       (if (not (empty? results))
         (list/table-paging (* limit (- page 1))
                            total
                            limit
                            #(repository-handler query (- (js/parseInt page) 1))
                            #(repository-handler query (+ (js/parseInt page) 1))))))])

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [{:keys [repository searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)]
    [:div.form-edit
     (form-repository repository)
     (form-list searching? repositories)]))