(ns swarmpit.component.service.create-image-repository
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :service :wizard :image :repository])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn repository-handler
  [user page]
  (ajax/GET (routes/path-for-backend :dockerhub-user-repo {:username user})
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repositoryPage page}
             :finally (state/update-value [:loading] true cursor)
             :handler (fn [response]
                        (let [res (keywordize-keys response)]
                          (state/update-value [:loading] false cursor)
                          (state/update-value [:data] res cursor)))}))

(defn- form-username [user users]
  (comp/form-comp
    "USERNAME"
    (comp/select-field
      {:value    user
       :onChange (fn [_ _ v]
                   (state/update-value [:data] [] cursor)
                   (state/update-value [:user] v cursor)
                   (repository-handler v 1))}
      (->> users
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(rum/defc form-loading < rum/static []
  (comp/form-comp-loading true))

(rum/defc form-loaded < rum/static []
  (comp/form-comp-loading false))

(defn- repository-list [user data]
  (let [{:keys [results page limit total]} data
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
        (comp/list-table-header ["Name" "Description"])
        (comp/list-table-body results
                              render-item
                              [[:name] [:description]])
        (if (not (empty? results))
          (comp/list-table-paging offset
                                  total
                                  limit
                                  #(repository-handler user (- (js/parseInt page) 1))
                                  #(repository-handler user (+ (js/parseInt page) 1))))))))

(rum/defc form < rum/reactive [users]
  (let [{:keys [loading
                user
                data]} (state/react cursor)]
    (if (some? user)
      [:div.form-edit
       (form-username user users)
       [:div.form-edit-loader
        (if loading
          (form-loading)
          (form-loaded))
        (repository-list user data)]]
      [:div.form-edit "No docker user found. Please ask your admin to create some :)"])))

