(ns swarmpit.component.service.create-image-public
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.handler :as handler]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(def cursor [:form :public])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defonce searching? (atom false))

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-handler
  [query page]
  (handler/get
    (routes/path-for-backend :public-repositories)
    {:params     {:query query
                  :page  page}
     :state      searching?
     :on-success (fn [response]
                   (state/update-value [:repositories] response cursor))}))

(defn- form-repository [repository]
  (form/comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Find repository"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor)
                   (repository-handler v 1))})))

(defn- init-state
  []
  (state/set-value {:repositories []
                    :repository   ""} cursor))

(def mixin-init-form
  (mixin/init-form-tab
    (fn []
      (init-state))))

(rum/defc form-list < rum/static [searching? {:keys [results page limit total query]}]
  (let [offset (* limit (- page 1))
        repository (fn [index] (:name (nth results index)))]
    [:div.form-edit-loader
     (if searching?
       (progress/loading)
       (progress/loaded))
     (comp/mui
       (comp/table
         {:key         "tbl"
          :selectable  false
          :onCellClick (fn [i]
                         (dispatch!
                           (routes/path-for-frontend :service-create-config
                                                     {}
                                                     {:repository (repository i)})))}
         (list/table-header headers)
         (list/table-body headers
                          results
                          render-item
                          [[:name] [:description]])
         (if (not (empty? results))
           (list/table-paging offset
                              total
                              limit
                              #(repository-handler query (- (js/parseInt page) 1))
                              #(repository-handler query (+ (js/parseInt page) 1))))))]))

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [{:keys [repository repositories]} (state/react cursor)]
    [:div.form-edit
     (form-repository repository)
     (form-list (rum/react searching?) repositories)]))