(ns swarmpit.component.menu
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :as url]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def swarmpit-home-page "https://swarmpit.io")

(def swarmpit-revision-page "https://github.com/swarmpit/swarmpit/commit")

(def menu
  [{:name    "Registries"
    :icon    (icon/cloud {})
    :handler :registry-list
    :domain  :registry}
   {:name "APPLICATIONS"}
   {:name    "Stacks"
    :icon    (comp/svg icon/stacks-path)
    :handler :stack-list
    :domain  :stack}
   {:name    "Services"
    :icon    (comp/svg icon/services-path)
    :handler :service-list
    :domain  :service}
   {:name    "Tasks"
    :icon    (comp/svg icon/tasks-path)
    :handler :task-list
    :domain  :task}
   {:name "INFRASTRUCTURE"}
   {:name    "Networks"
    :icon    (comp/svg icon/networks-path)
    :handler :network-list
    :domain  :network}
   {:name    "Nodes"
    :icon    (icon/computer {})
    :handler :node-list
    :domain  :node}
   {:name "DATA"}
   {:name    "Volumes"
    :icon    (icon/storage {})
    :handler :volume-list
    :domain  :volume}
   {:name    "Secrets"
    :icon    (comp/svg icon/secrets-path)
    :handler :secret-list
    :route   "secrets"
    :domain  :secret}
   {:name    "Configs"
    :icon    (comp/svg icon/configs-path)
    :handler :config-list
    :route   "configs"
    :domain  :config}])

(def admin-menu
  [{:name "ADMIN"}
   {:name    "Users"
    :icon    (comp/svg icon/users-path)
    :handler :user-list
    :domain  :user}])

(defn- parse-version [version]
  (clojure.string/replace
    (:version version)
    #"SNAPSHOT"
    (->> (:revision version)
         (take 7)
         (apply str))))

(defn- filter-menu [docker-api]
  (if (<= 1.30 docker-api)
    menu
    (filter #(not= :config (:domain %)) menu)))

(defn- version-handler
  []
  (ajax/get
    (routes/path-for-backend :version)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:version] response state/layout-cursor)
                   (state/set-value response))}))

(rum/defc drawer-title-name < rum/static []
  [:a {:target "_blank"
       :href   swarmpit-home-page}
   [:span.Swarmpit-title-name "Swarmpit"]])

(rum/defc drawer-title-version < rum/static [version]
  (when version
    [:a {:target "_blank"
         :href   (str swarmpit-revision-page "/" (:revision version))}
     [:span.Swarmpit-title-version (parse-version version)]]))

(rum/defc drawer-category < rum/static [name]
  (comp/list-item
    {:disabled  true
     :className "Swarmpit-drawer-category"
     :key       (str "drawer-category-" name)}
    (comp/list-item-text
      {:primary   name
       :className "Swarmpit-drawer-category-text"
       :key       (str "drawer-category-text-" name)})))

(rum/defc drawer-item < rum/static [name icon handler domain selected?]
  (comp/list-item
    (merge {:button    true
            :dense     true
            :className "Swarmpit-drawer-item"
            :key       (str "drawer-item-" name)
            :onClick   (fn []
                         (state/update-value [:mobileOpened] false state/layout-cursor)
                         (url/dispatch! (routes/path-for-frontend handler)))}
           (when selected?
             {:className "Swarmpit-drawer-item-selected"})
           (when (= :registry domain)
             {:style {:marginTop "10px"}}))
    (comp/list-item-icon
      (merge {:color "primary"
              :key   (str "drawer-item-icon-" name)}
             (if selected?
               {:className "Swarmpit-drawer-item-icon-selected"}
               {:className "Swarmpit-drawer-item-icon"})) icon)
    (comp/list-item-text
      (merge {:primary (comp/typography {:variant "subtitle2"} name)
              :key     (str "drawer-item-text-" name)}
             (if selected?
               {:className "Swarmpit-drawer-item-text-selected"}
               {:className "Swarmpit-drawer-item-text"})))))

(def retrieve-version
  {:init
   (fn [state]
     (version-handler)
     state)})

(rum/defc drawer-content < rum/static [version page-domain docker-api]
  [:div.Swarmpit-drawer-content
   (html
     [:div.Swarmpit-toolbar
      [:div.Swarmpit-title
       (drawer-title-name)
       (drawer-title-version version)]])
   (comp/divider)
   (map
     (fn [{:keys [icon name handler domain]}]
       (let [selected? (= page-domain domain)]
         (rum/with-key
           (if (some? icon)
             (drawer-item name icon handler domain selected?)
             (drawer-category name))
           name)))
     (let [fmenu (filter-menu docker-api)]
       (if (storage/admin?)
         (concat fmenu admin-menu)
         fmenu)))])

(rum/defc drawer < rum/reactive
                   retrieve-version [page-domain]
  (let [{:keys [mobileOpened version]} (state/react state/layout-cursor)
        docker-api (state/react state/docker-api-cursor)]
    (comp/mui
      (html
        [:div
         (comp/hidden
           {:mdUp true}
           (comp/drawer
             {:key        "drawer"
              :className  "Swarmpit-drawer"
              :anchor     "left"
              :open       mobileOpened
              :variant    "temporary"
              :onClose    #(state/update-value [:mobileOpened] false state/layout-cursor)
              :ModalProps {:keepMounted true}}
             (rum/with-key
               (drawer-content version page-domain docker-api)
               "drawer-content")))
         (comp/hidden
           {:smDown         true
            :implementation "css"}
           (comp/drawer
             {:key       "drawer"
              :className "Swarmpit-drawer"
              :open      true
              :variant   "permanent"}
             (rum/with-key
               (drawer-content version page-domain docker-api)
               "drawer-content")))]))))
