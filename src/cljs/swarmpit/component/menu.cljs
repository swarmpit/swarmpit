(ns swarmpit.component.menu
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.common :as common]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [swarmpit.url :as url]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def menu
  [{:name    "Dashboard"
    :icon    (icon/dashboard {})
    :handler :index
    :domain  :index}
   {:name    "Registries"
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

(defn footer []
  (comp/box
    {:className "Swarmpit-drawer-footer"}
    (comp/divider {:className "Swarmpit-drawer-divider"})
    (comp/list-item
      {:button        true
       :component     "a"
       :href          "/api-docs"
       :target        "_blank"
       :className     "Swarmpit-drawer-item"
       :dense         true
       :disableRipple true}
      (comp/list-item-text
        {:className "Swarmpit-drawer-footer-item-text"
         :primary   (comp/typography {:variant "subtitle2"} "Swagger API")})
      (comp/list-item-icon
        {:color "primary"} (icon/open-in-new {:style {:fontSize 15}})))))

(defn- filter-menu [docker-api]
  (if (<= 1.30 docker-api)
    menu
    (filter #(not= :config (:domain %)) menu)))

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
    (merge {:button        true
            :component     "a"
            :href          (routes/path-for-frontend handler)
            :dense         true
            :disableRipple true
            :className     "Swarmpit-drawer-item"
            :key           (str "drawer-item-" name)
            :onClick       (fn []
                             (state/update-value [:mobileOpened] false state/layout-cursor)
                             (url/dispatch! (routes/path-for-frontend handler)))}
           (when selected?
             {:className "Swarmpit-drawer-item-selected"})
           (when (= :index domain)
             {:style {:marginTop "1rem"}}))
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

(rum/defc drawer-content < rum/static [version page-domain docker-api]
  [:div.Swarmpit-drawer-content
   (comp/box
     {:className "Swarmpit-toolbar"}
     (comp/box
       {:className "Swarmpit-menu-title"}
       (common/title-logo)
       (common/title-version version)))
   (comp/box
     {}
     (map
       (fn [{:keys [icon name handler domain]}]
         (let [selected? (= page-domain domain)]
           (rum/with-key
             (if (some? icon)
               (drawer-item name icon handler domain selected?)
               (drawer-category name))
             name)))
       (filter-menu docker-api)))
   (comp/box {:className "grow"})
   (footer)])

(rum/defc drawer < rum/reactive [page-domain]
  (let [{:keys [mobileOpened version]} (state/react state/layout-cursor)
        docker-api (state/react state/docker-api-cursor)]
    (comp/mui
      (html
        [:div
         (comp/hidden
           {:lgUp true}
           (comp/drawer
             {:className  "Swarmpit-drawer"
              :anchor     "left"
              :open       mobileOpened
              :variant    "temporary"
              :onClose    #(state/update-value [:mobileOpened] false state/layout-cursor)
              :ModalProps {:keepMounted true}}
             (drawer-content version page-domain docker-api)))
         (comp/hidden
           {:mdDown         true
            :implementation "css"}
           (comp/drawer
             {:className "Swarmpit-drawer"
              :open      true
              :variant   "permanent"}
             (drawer-content version page-domain docker-api)))]))))