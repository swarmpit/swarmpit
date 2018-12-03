(ns swarmpit.component.service.info.settings
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.chart :as chart]
            [material.component.label :as label]
            [swarmpit.docker.utils :as utils]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.inflect :as inflect]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(defonce digest-shown (atom false))

(defn resource-provided?
  [resource]
  (let [cpu (:cpu resource)
        memory (:memory resource)]
    (or (> cpu 0)
        (> memory 0))))

(rum/defc form-subheader < rum/reactive [image image-digest]
  (if image-digest
    (comp/click-away-listener
      {:onClickAway #(reset! digest-shown false)}
      (comp/tooltip
        {:PopperProps          {:disablePortal true}
         :onClose              #(reset! digest-shown false)
         :open                 (rum/react digest-shown)
         :disableFocusListener true
         :disableHoverListener true
         :disableTouchListener true
         :title                image-digest}
        (html [:span {:onClick #(reset! digest-shown true)
                      :style   {:cursor "pointer"}} image])))
    (html [:span image])))

(rum/defc form-replicas < rum/static [tasks]
  (let [data (->> (range 0 (:total tasks))
                  (map (fn [num]
                         (if (< num (:running tasks))
                           {:name  (str "Replica " (inc num))
                            :value 1
                            :color "#43a047"}
                           {:name  (str "Replica " (inc num))
                            :value 1
                            :color "#6c757d"})))
                  (into []))]
    (chart/pie
      data
      (str (:total tasks) " " (inflect/pluralize-noun (:total tasks) "replica"))
      "Swarmpit-service-replicas-graph"
      "replicas-pie")))

(defn- form-command [command]
  (when command
    (html [:pre {:style {:fontSize "0.9em"
                         :margin   0}}
           (let [merged (str/join " " command)]
             (if (< 100 (count merged))
               (str/join "\n" command)
               merged))])))

(defn- form-state [state]
  (case state
    "running" (label/green state)
    "not running" (label/info state)
    "partly running" (label/yellow state)))

(rum/defc form-dashboard < rum/static [{:keys [limit reservation]} tasks]
  (html
    (cond
      (and (resource-provided? reservation)
           (resource-provided? limit))
      [:table
       [:tr
        [:td {:rowspan 2} (form-replicas tasks)]
        [:td
         [:div "Reservation"]
         [:div
          [:span [:b "CPU: "]]
          [:span (:cpu reservation)]]
         [:div
          [:span [:b "MEMORY: "]]
          [:span (str (:memory reservation) "MB")]]]]
       [:tr
        [:td
         [:div "Limit"]
         [:div
          [:span [:b "CPU: "]]
          [:span (:cpu limit)]]
         [:div
          [:span [:b "MEMORY: "]]
          [:span (str (:memory limit) "MB")]]]]]
      (resource-provided? reservation)
      [:table
       [:tr
        [:td (form-replicas tasks)]
        [:td
         [:div "Reservation"]
         [:div
          [:span [:b "CPU: "]]
          [:span (:cpu reservation)]]
         [:div
          [:span [:b "MEMORY: "]]
          [:span (str (:memory reservation) "MB")]]]]]
      (resource-provided? limit)
      [:table
       [:tr
        [:td (form-replicas tasks)]
        [:td
         [:div "Limit"]
         [:div
          [:span [:b "CPU: "]]
          [:span (:cpu limit)]]
         [:div
          [:span [:b "MEMORY: "]]
          [:span (str (:memory limit) "MB")]]]]]
      :else (form-replicas tasks))))

(rum/defc form < rum/static [service]
  (let [image-digest (get-in service [:repository :imageDigest])
        image (get-in service [:repository :image])
        tasks (get-in service [:status :tasks])
        resources (:resources service)
        command (:command service)
        stack (:stack service)
        mode (:mode service)]
    (comp/card
      {:className "Swarmpit-form-card"}
      (comp/card-header
        {:title     (utils/trim-stack stack (:serviceName service))
         :className "Swarmpit-form-card-header"
         :subheader (form-subheader image image-digest)})
      (comp/card-content
        {}
        (form-dashboard resources tasks)
        (form-command command))
      (comp/card-content
        {}
        (form/item-labels
          [(form-state (:state service))
           (label/grey mode)]))
      (comp/card-actions
        {}
        (when stack
          (comp/button
            {:size  "small"
             :color "primary"
             :href  (routes/path-for-frontend :stack-info {:name stack})}
            "See stack")))
      (comp/divider)
      (comp/card-content
        {:style {:paddingBottom "16px"}}
        (comp/typography
          {:color "textSecondary"
           :style {:flexDirection "column"}}
          (form/item-date (:createdAt service)
                          (:updatedAt service)))
        (comp/typography
          {:color "textSecondary"
           :style {:flexDirection "column"}}
          (form/item-id (:id service)))))))