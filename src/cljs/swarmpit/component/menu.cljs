(ns swarmpit.component.menu
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def swarmpit-home-page "https://swarmpit.io")

(def swarmpit-revision-page "https://github.com/swarmpit/swarmpit/commit")

(def drawer-width 200)

(def styles
  (let [theme (js->clj comp/theme)
        spacing-unit (get-in theme ["spacing" "unit"])
        breakpoints-up (get-in theme ["breakpoints" "up"])
        transitions (-> theme (get "transitions"))
        transitions-create (get transitions "create")
        transition-easing (-> transitions (get-in ["easing" "sharp"]))
        transition-duration-leaving (-> transitions (get-in ["duration" "leavingScreen"]))
        transition-duration-entering (-> transitions (get-in ["duration" "enteringScreen"]))]
    {:drawerPaper      {:position   "relative"
                        :whiteSpace "nowrap"
                        :width      drawer-width
                        :transition (transitions-create
                                      "width"
                                      (clj->js {:easing   transition-easing
                                                :duration transition-duration-entering}))}
     :drawerPaperClose {:overflowX  "hidden"
                        :transition (transitions-create
                                      "width"
                                      (clj->js {:easing   transition-easing
                                                :duration transition-duration-leaving}))
                        :width      (* spacing-unit 7)
                        (breakpoints-up "sm")
                                    {:width (* spacing-unit 9)}}
     :toolbar          (merge {:display        "flex"
                               :alignItems     "center"
                               :justifyContent "flex-end"
                               :padding        "0 8px"}
                              (get-in theme ["mixins" "toolbar"]))}))

(def menu
  [{:name "APPLICATIONS"}
   {:name    "Stacks"
    :icon    (comp/svg icon/stacks)
    :handler :stack-list
    :domain  :stack}
   {:name    "Services"
    :icon    (comp/svg icon/services)
    :handler :service-list
    :domain  :service}
   {:name    "Tasks"
    :icon    (comp/svg icon/tasks)
    :handler :task-list
    :domain  :task}
   {:name "INFRASTRUCTURE"}
   {:name    "Networks"
    :icon    (comp/svg icon/networks)
    :handler :network-list
    :domain  :network}
   {:name    "Nodes"
    :icon    (comp/svg icon/nodes)
    :handler :node-list
    :domain  :node}
   {:name "DATA"}
   {:name    "Volumes"
    :icon    (comp/svg icon/volumes)
    :handler :volume-list
    :domain  :volume}
   {:name    "Secrets"
    :icon    (comp/svg icon/secrets)
    :handler :secret-list
    :route   "secrets"
    :domain  :secret}
   {:name    "Configs"
    :icon    (comp/svg icon/configs)
    :handler :config-list
    :route   "configs"
    :domain  :config}
   {:name "DISTRIBUTION"}
   {:name    "Dockerhub"
    :icon    (comp/svg icon/docker)
    :handler :dockerhub-user-list
    :domain  :dockerhub}
   {:name    "Registry"
    :icon    (comp/svg icon/registries)
    :handler :registry-list
    :domain  :registry}])

(def admin-menu
  [{:name "ADMIN"}
   {:name    "Users"
    :icon    (comp/svg icon/users)
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

(rum/defc drawer-category < rum/static [name opened?]
  (comp/list-item
    {:disabled true
     :key      (str "Swarmpit-drawer-category-" name)}
    (comp/list-item-text
      (merge {:primary   name
              :className "Swarmpit-drawer-category-text"
              :key       (str "Swarmpit-drawer-category-text-" name)}
             (when (false? opened?)
               {:className "hide"})))))

(rum/defc drawer-item < rum/static [name icon handler selected?]
  (comp/list-item
    (merge {:button  true
            :key     (str "Swarmpit-drawer-item-" name)
            :onClick #(dispatch! (routes/path-for-frontend handler))}
           (when selected?
             {:className "Swarmpit-drawer-item-selected"}))
    (comp/list-item-icon
      (merge {:color "primary"
              :key   (str "Swarmpit-drawer-item-icon-" name)}
             (if selected?
               {:className "Swarmpit-drawer-item-icon-selected"}
               {:className "Swarmpit-drawer-item-icon"})) icon)
    (comp/list-item-text
      (merge {:primary name
              :key     (str "Swarmpit-drawer-item-text-" name)}
             (if selected?
               {:className "Swarmpit-drawer-item-text-selected"}
               {:className "Swarmpit-drawer-item-text"})))))

(def retrieve-version
  {:init
   (fn [state]
     (version-handler)
     state)})

(rum/defc drawer < rum/reactive
                   retrieve-version [page-domain]
  (let [{:keys [opened version]} (state/react state/layout-cursor)
        docker-api (state/react state/docker-api-cursor)]
    (comp/mui
      (comp/drawer
        {:key       "Swarmpit-drawer"
         :open      opened
         :className (if opened
                      "Swarmpit-drawer"
                      "Swarmpit-drawer Swarmpit-drawer-closed")
         :variant   "permanent"}
        (html
          [:div.Swarmpit-toolbar
           [:div.Swarmpit-title
            (drawer-title-name)
            (drawer-title-version version)]
           (comp/icon-button
             {:onClick #(state/update-value [:opened] false state/layout-cursor)}
             icon/chevron-left)])
        (comp/divider)
        (map
          (fn [menu-item]
            (let [icon (:icon menu-item)
                  name (:name menu-item)
                  handler (:handler menu-item)
                  domain (:domain menu-item)
                  selected (= page-domain domain)]
              (if (some? icon)
                (drawer-item name icon handler selected)
                (drawer-category name opened))))
          (let [fmenu (filter-menu docker-api)]
            (if (storage/admin?)
              (concat fmenu admin-menu)
              fmenu)))))))