(ns swarmpit.component.service.create-image-private
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [swarmpit.ajax :as ajax]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def form-value-cursor (conj state/form-value-cursor :private))

(def form-state-cursor (conj state/form-state-cursor :private))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- selected-user
  [user-id users]
  (-> (filter #(= user-id (:_id %)) users)
      (first)))

(defn- onclick-handler
  [repository]
  (dispatch!
    (routes/path-for-frontend :service-create-config
                              {}
                              {:repository repository})))

(defn- repository-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :dockerhub-repositories {:id user-id})
    {:state      [:private :searching?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response form-value-cursor))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Repositories fetching failed. " (:error response))))}))

(defn- form-username-label
  [user]
  (if (= (storage/user)
         (:owner user))
    (:username user)
    (html [:span (:username user)
           [:span.Swarmpit-image-owner (str " [" (:owner user) "]")]])))

(defn- form-username [user users]
  (comp/text-field
    {:fullWidth       true
     :id              "user"
     :label           "Docker user"
     :select          true
     :value           (:_id user)
     :margin          "normal"
     :variant         "outlined"
     :style           {:maxWidth "400px"}
     :InputLabelProps {:shrink true}
     :onChange        (fn [event]
                        (let [value (-> event .-target .-value)]
                          (state/update-value [:user] (selected-user value users) form-state-cursor)
                          (repository-handler value)))}
    (->> users
         (map #(comp/menu-item
                 {:key   (:_id %)
                  :value (:_id %)} (form-username-label %))))))

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
                        (state/update-value [:repository] (-> event .-target .-value) form-state-cursor))}))

(defn- init-form-state
  [user]
  (state/set-value {:searching? false
                    :repository ""
                    :user       user} form-state-cursor))

(def mixin-init-form
  (mixin/init-tab
    (fn [users]
      (let [init-user (first users)]
        (init-form-state init-user)
        (when (some? init-user)
          (repository-handler (:_id init-user)))))))

(def render-list-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:description item))})

(rum/defc form-list < rum/reactive []
  (let [{:keys [repository searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)
        filtered-repositories (filter-items repositories repository)]
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
               (last filtered-repositories)
               #(onclick-handler (:name item)))) filtered-repositories))])))

(rum/defc form < rum/reactive
                 mixin-init-form [users]
  (let [{:keys [user repository]} (state/react form-state-cursor)]
    (if (some? user)
      [:div.Swarmpit-image-search
       (form-username user users)
       (form-repository repository)]
      [:div.Swarmpit-image-search
       (html
         [:span.Swarmpit-message
          (icon/info {:style {:marginRight "8px"}})
          (if (storage/admin?)
            [:span "No dockerhub users found. Add new " [:a {:href (routes/path-for-frontend :dockerhub-user-create)} "user."]]
            [:span "No dockerhub users found. Please ask your admin to setup."])])])))