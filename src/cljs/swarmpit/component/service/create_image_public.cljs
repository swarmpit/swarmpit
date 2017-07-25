(ns swarmpit.component.service.create-image-public
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(def cursor [:page :service :wizard :image :public])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-handler
  [query page]
  (handler/get
    (routes/path-for-backend :dockerhub-repositories)
    {:params     {:query query
                  :page  page}
     :on-call    (state/update-value [:searching] true cursor)
     :on-success (fn [response]
                   (state/update-value [:searching] false cursor)
                   (state/update-value [:data] response cursor))
     :on-error   (fn [_]
                   (state/update-value [:searching] false cursor))}))

(defn- form-repository [repository]
  (comp/form-comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Find repository"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor)
                   (repository-handler v 1))})))

(rum/defc form-loading < rum/static []
  (comp/form-comp-loading true))

(rum/defc form-loaded < rum/static []
  (comp/form-comp-loading false))

(defn- repository-list [data]
  (let [{:keys [results page limit total query]} data
        offset (* limit (- page 1))
        repository (fn [index] (:name (nth results index)))]
    (comp/mui
      (comp/table
        {:key         "tbl"
         :selectable  false
         :onCellClick (fn [i]
                        (dispatch!
                          (routes/path-for-frontend :service-create-config
                                                    {}
                                                    {:repository (repository i)
                                                     :registry   "dockerhub"})))}
        (comp/list-table-header headers)
        (comp/list-table-body headers
                              results
                              render-item
                              [[:name] [:description]])
        (if (not (empty? results))
          (comp/list-table-paging offset
                                  total
                                  limit
                                  #(repository-handler query (- (js/parseInt page) 1))
                                  #(repository-handler query (+ (js/parseInt page) 1))))))))

(rum/defc form < rum/reactive []
  (let [{:keys [searching
                repository
                data]} (state/react cursor)]
    [:div.form-edit
     (form-repository repository)
     [:div.form-edit-loader
      (if searching
        (form-loading)
        (form-loaded))
      (repository-list data)]]))