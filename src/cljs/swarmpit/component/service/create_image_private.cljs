(ns swarmpit.component.service.create-image-private
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def form-value-cursor (conj state/form-value-cursor :private))

(def form-state-cursor (conj state/form-state-cursor :private))

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defn- render-item
  [item]
  (val item))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- selected-user
  [user-id users]
  (-> (filter #(= user-id (:_id %)) users)
      (first)))

(defn- onclick-handler
  [index repositories]
  (let [repository (fn [index] (:name (nth repositories index)))]
    (dispatch!
      (routes/path-for-frontend :service-create-config
                                {}
                                {:repository (repository index)}))))

(defn- repository-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :dockerhub-repositories {:id user-id})
    {:state      [:private :searching?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response form-value-cursor))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Repositories fetching failed. Reason: " (:error response))))}))

(defn- form-username-label
  [user]
  (if (= (storage/user)
         (:owner user))
    (:username user)
    (html [:span (:username user)
           [:span.owner-item (str " [" (:owner user) "]")]])))

(defn- form-username [user users]
  (form/comp
    "DOCKER USER"
    (comp/select-field
      {:value    (:_id user)
       :onChange (fn [_ _ v]
                   (state/update-value [:user] (selected-user v users) form-state-cursor)
                   (repository-handler v))}
      (->> users
           (map #(comp/menu-item
                   {:key         (:_id %)
                    :value       (:_id %)
                    :primaryText (form-username-label %)}))))))

(defn- form-repository [repository]
  (form/comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Filter by name"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v form-state-cursor))})))

(defn- init-form-state
  [user]
  (state/set-value {:searching? false
                    :repository ""
                    :user       user} form-state-cursor))

(def mixin-init-form
  (mixin/init-tab
    (fn [users]
      (init-form-state (first users)))))

(rum/defc form-list < rum/static [searching? repositories]
  [:div.form-edit-loader
   (if searching?
     (progress/loading)
     (progress/loaded))
   (comp/mui
     (comp/table
       {:key         "tbl"
        :selectable  false
        :onCellClick (fn [i] (onclick-handler i repositories))}
       (list/table-header headers)
       (list/table-body headers
                        repositories
                        render-item
                        [[:name] [:description]])))])

(rum/defc form < rum/reactive
                 mixin-init-form [users]
  (let [{:keys [user repository searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)
        filtered-repositories (filter-items repositories repository)]
    (if (some? user)
      [:div.form-edit
       (form-username user users)
       (form-repository repository)
       (form-list searching? filtered-repositories)]
      [:div.form-edit
       (if (storage/admin?)
         (form/icon-value icon/info [:span "No dockerhub users found. Add new " [:a {:href (routes/path-for-frontend :dockerhub-user-create)} "user."]])
         (form/icon-value icon/info "No dockerhub users found. Please ask your admin to setup."))])))