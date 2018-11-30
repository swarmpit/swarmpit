(ns swarmpit.component.service.create-image-private
  (:require [material.icon :as icon]
            [material.components :as comp]
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
           [:span.owner-item (str " [" (:owner user) "]")]])))

(defn- form-username [user users]
  (comp/text-field
    {:fullWidth       true
     :id              "user"
     :label           "Docker user"
     :select          true
     :value           (:_id user)
     :margin          "normal"
     :variant         "outlined"
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
     :value           repository
     :margin          "normal"
     :variant         "outlined"
     :placeholder     "Find repository"
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

(rum/defc form-list < rum/static [searching? repositories]
  (html
    [:div
     (when searching?
       (comp/linear-progress))
     (comp/list
       {:dense true}
       (->> repositories
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
                 mixin-init-form [users]
  (let [{:keys [user repository searching?]} (state/react form-state-cursor)
        repositories (state/react form-value-cursor)
        filtered-repositories (filter-items repositories repository)]
    (if (some? user)
      [:div.Swarmpit-image-search
       (form-username user users)
       (form-repository repository)
       [:span.Swarmpit-message (str "Found " (count filtered-repositories) " repositories.")]
       (form-list searching? filtered-repositories)]
      [:div.Swarmpit-image-search
       (html
         [:span.Swarmpit-message
          icon/info
          (if (storage/admin?)
            [:span "No dockerhub users found. Add new " [:a {:href (routes/path-for-frontend :dockerhub-user-create)} "user."]]
            [:span "No dockerhub users found. Please ask your admin to setup."])])])))