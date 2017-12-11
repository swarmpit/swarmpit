(ns swarmpit.component.service.create-image-private
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def cursor [:form :private])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defonce searching? (atom false))

(defn- render-item
  [item]
  (val item))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- repository-handler
  [user-id]
  (handler/get
    (routes/path-for-backend :dockerhub-repositories {:id user-id})
    {:state      searching?
     :on-success (fn [response]
                   (state/update-value [:repositories] response cursor))}))

(defn- form-username-label
  [user]
  (if (= (storage/user)
         (:owner user))
    (:username user)
    (html [:span (:username user)
           [:span.owner-item (str " [" (:owner user) "]")]])))

(defn- form-username [user users]
  (let [user-by-id (fn [id] (first (filter #(= id (:_id %)) users)))]
    (form/comp
      "DOCKER USER"
      (comp/select-field
        {:value    (:_id user)
         :onChange (fn [_ _ v]
                     (state/update-value [:data] [] cursor)
                     (state/update-value [:user] (user-by-id v) cursor)
                     (repository-handler v))}
        (->> users
             (map #(comp/menu-item
                     {:key         (:_id %)
                      :value       (:_id %)
                      :primaryText (form-username-label %)})))))))

(defn- form-repository [repository]
  (form/comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Filter by name"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor))})))

(defn- init-state
  [user]
  (state/set-value {:repositories []
                    :repository   ""
                    :user         user} cursor))

(def mixin-init-form
  (mixin/init-form-tab
    (fn [users]
      (init-state (first users)))))

(rum/defc form-list < rum/static [searching? user repositories]
  (let [repository (fn [index] (:name (nth repositories index)))]
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
                                                     {:repository       (repository i)
                                                      :distributionType "dockerhub"
                                                      :distribution     (:_id user)})))}
         (list/table-header headers)
         (list/table-body headers
                          repositories
                          render-item
                          [[:name] [:description]])))]))

(rum/defc form < rum/reactive
                 mixin-init-form [users]
  (let [{:keys [user repositories repository]} (state/react cursor)
        filtered-repositories (filter-items repositories repository)]
    (if (some? user)
      [:div.form-edit
       (form-username user users)
       (form-repository repository)
       (form-list (rum/react searching?) user filtered-repositories)]
      [:div.form-edit
       (if (storage/admin?)
         (form/icon-value icon/info [:span "No dockerhub users found. Add new " [:a {:href (routes/path-for-frontend :dockerhub-user-create)} "user."]])
         (form/icon-value icon/info "No dockerhub users found. Please ask your admin to setup."))])))