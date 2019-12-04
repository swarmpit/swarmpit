(ns swarmpit.component.service.info.settings
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.chart :as chart]
            [material.component.label :as label]
            [swarmpit.component.common :as common]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.state :as state]
            [swarmpit.docker.utils :as utils]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.inflect :as inflect]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form-replicas < rum/static [tasks]
  (let [data (->> tasks
                  (map (fn [task]
                         (if (= "running" (:state task))
                           {:name  (:taskName task)
                            :value 1
                            :color "#43a047"
                            :state (:state task)}
                           {:name  (:taskName task)
                            :value 1
                            :color "#6c757d"
                            :state (:state task)})))
                  (into []))]
    (chart/pie
      data
      (str (count tasks) " " (inflect/pluralize-noun (count tasks) "replica"))
      "Swarmpit-service-replicas-graph"
      "replicas-pie"
      {:formatter (fn [value name props]
                    (.-state (.-payload props)))})))

(rum/defc form-registry < rum/static [registry]
  (html
    [:span
     [:span {:style {:fontWeight 700}} "FROM "] registry]))

(rum/defc form-command < rum/reactive [command]
  (let [{:keys [cmdAnchor cmdShow]} (state/react state/form-state-cursor)]
    (html
      [:div.Swarmpit-commmand
       {:ref (fn [node]
               (when (and node (nil? cmdAnchor))
                 (state/update-value [:cmdAnchor] node state/form-state-cursor)))}
       (when command
         (comp/svg {:style {:marginRight "8px"}} icon/terminal-path))
       (when command
         [:div
          [:div.Swarmpit-command-text
           (map-indexed
             (fn [index item]
               (if (< index 5)
                 (html [:pre {:key index} item]))) command)]
          (when (> (count command) 4)
            [:div
             [:a
              {:onClick   #(state/update-value [:cmdShow] true state/form-state-cursor)
               :className "link"} "See more..."]
             (comp/popover
               {:open            (and cmdAnchor cmdShow)
                :anchorEl        cmdAnchor
                :onClick         #(state/update-value [:cmdShow] false state/form-state-cursor)
                :onClose         #(state/update-value [:cmdShow] false state/form-state-cursor)
                :anchorOrigin    {:vertical   "top"
                                  :horizontal "left"}
                :transformOrigin {:vertical   "top"
                                  :horizontal "left"}}
               (html
                 [:div.Swarmpit-command-text.Swarmpit-popover
                  (map-indexed
                    (fn [index item]
                      (html [:pre {:key index} item])) command)]))])])])))

(defn- form-state [state]
  (case state
    "running" (label/header state "green")
    "not running" (label/header state "info")
    "partly running" (label/header "running" "yellow")))

(defn- autoredeploy-label
  [autoredeploy]
  (when autoredeploy (label/header "autoredeploy" "primary")))

(defn- agent-label
  [agent]
  (when agent (label/header "agent" "primary")))

(rum/defc form < rum/reactive [service tasks]
  (let [image-digest (get-in service [:repository :imageDigest])
        image (get-in service [:repository :image])
        logdriver (get-in service [:logdriver :name])
        desired-tasks (filter #(not= "shutdown" (:desiredState %)) tasks)
        registry (utils/linked-registry image)
        command (:command service)
        stack (:stack service)
        links (:links service)
        mode (:mode service)]
    (comp/card
      {:className "Swarmpit-form-card"}

      (comp/card-header
        {:title     (comp/typography {:variant "h6"} "Summary")
         :subheader (form/item-labels
                      [(form-state (:state service))
                       (agent-label (:agent service))
                       (autoredeploy-label (-> service :deployment :autoredeploy))
                       (label/header mode "grey")])})
      (comp/card-content
        {}
        (if (not (empty? desired-tasks))
          (form-replicas desired-tasks)
          (form/message "Service has been shut down."))
        (form-command command))
      (form/item-main "ID" (:id service) false)
      (form/item-main "Name" (:serviceName service))
      (when registry
        (form/item-main "Registry" registry))
      (form/item-main "Image" (if registry
                            (utils/registry-repository image registry)
                            image))
      (form/item-main "Created" (form/item-date (:createdAt service)))
      (form/item-main "Last update" (form/item-date (:updatedAt service)))
      (comp/divider {})
      (comp/card-actions
        {}
        (when stack
          (comp/button
            {:size  "small"
             :color "primary"
             :href  (routes/path-for-frontend :stack-info {:name stack})}
            "See stack"))
        (comp/button
          {:size     "small"
           :color    "primary"
           :disabled (not (contains? #{"json-file" "journald"} logdriver))
           :href     (routes/path-for-frontend :service-log {:id (:serviceName service)})}
          "View log")
        (->> links
             (map #(comp/button
                     {:size   "small"
                      :color  "primary"
                      :target "_blank"
                      :href   (:value %)}
                     (str/replace (:name %) #"_" " "))))))))