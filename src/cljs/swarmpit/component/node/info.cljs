(ns swarmpit.component.node.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.contrib.humanize :as humanize]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn resources
  [node]
  (let [cpu (-> node :resources :cpu (int))
        memory-bytes (-> node :resources :memory (* 1024 1024))
        disk-bytes (-> node :stats :disk :total)]
    (let [core-stats (str cpu " " (clojure.contrib.inflect/pluralize-noun cpu "core") ", "
                          (humanize/filesize memory-bytes :binary true) " memory")]
      (if (some? disk-bytes)
        (str core-stats ", " (humanize/filesize disk-bytes :binary true) " disk")
        core-stats))))

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(rum/defc form-general < rum/static [node]
  (comp/card
    {:className "Swarmpit-form-card"
     :key       "ngc"}
    (comp/card-header
      {:title     (:nodeName node)
       :className "Swarmpit-form-card-header"
       :key       "ngch"
       :subheader (:address node)
       :action    (comp/tooltip
                    {:title "Edit node"
                     :key   "ngchaet"}
                    (comp/icon-button
                      {:aria-label "Edit"
                       :href       (routes/path-for-frontend :node-edit {:id (:id node)})}
                      (comp/svg icon/edit-path)))})
    (comp/card-content
      {:key "ngcc"}
      (html
        [:div {:key "ngccd"}
         [:span [:b "ENGINE: "] (str "docker " (:engine node))]
         [:br]
         [:span [:b "OS: "] [(:os node) " " (:arch node)]]
         [:br]
         [:span [:b "RESOURCES: "] (resources node)]]))
    (comp/card-content
      {:key "ngccl"}
      (form/item-labels
        [(node-item-state (:state node))
         (when (:leader node)
           (label/primary "Leader"))
         (label/grey (:role node))
         (if (= "active" (:availability node))
           (label/green "active")
           (label/grey (:availability node)))]))
    (comp/divider
      {:key "ncd"})
    (comp/card-content
      {:style {:paddingBottom "16px"}
       :key   "ngccf"}
      (form/item-id (:id node)))))

(def render-labels-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form-labels < rum/static [labels id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "nlc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "nlch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "labels-title"} "Labels")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :node-edit {:id id}
                                   {:section "Labels"})}
                    (comp/svg icon/edit-path))})
    (if (empty? labels)
      (comp/card-content
        {:key "nlcce"}
        (html [:div "No labels defined for the node."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "nlcc"}
        (rum/with-key
          (list/list
            render-labels-metadata
            labels
            nil) "nlcclc")))))

(rum/defc form-plugins < rum/static [networks volumes]
  (comp/card
    {:className "Swarmpit-card"
     :key       "npc"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :key       "npch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "plugins-title"} "Plugins")})
    (comp/card-content
      {:className "Swarmpit-form-card-content"
       :key       "npccn"}
      (form/subsection "Network")
      (map #(comp/chip
              {:label %
               :key   (str "np-" %)
               :style {:marginRight "5px"}}) networks))
    (comp/card-content
      {:className "Swarmpit-form-card-content"
       :key       "npccv"}
      (form/subsection "Volume")
      (map #(comp/chip
              {:label %
               :key   (str "vp-" %)
               :style {:marginRight "5px"}}) volumes))))

(rum/defc form-tasks < rum/static [tasks]
  (let [table-summary (->> (get-in tasks/render-metadata [:table :summary])
                           (filter #(not= "Node" (:name %)))
                           (into []))
        custom-metadata (assoc-in tasks/render-metadata [:table :summary] table-summary)]
    (comp/card
      {:className "Swarmpit-card"
       :key       "ftc"}
      (comp/card-header
        {:className "Swarmpit-table-card-header"
         :key       "ftch"
         :title     (comp/typography
                      {:variant "h6"
                       :key     "tasks-title"} "Tasks")})
      (if (empty? tasks)
        (comp/card-content
          {:key "ftcce"}
          (html [:div "No tasks running on the node."]))
        (comp/card-content
          {:className "Swarmpit-table-card-content"
           :key       "ftcc"}
          (rum/with-key
            (list/responsive
              custom-metadata
              (filter #(not (= "shutdown" (:state %))) tasks)
              tasks/onclick-handler) "ftccrl"))))))

(defn- node-tasks-handler
  [node-id]
  (ajax/get
    (routes/path-for-backend :node-tasks {:id node-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:tasks] response state/form-value-cursor))}))

(defn- node-handler
  [node-id]
  (ajax/get
    (routes/path-for-backend :node {:id node-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:node] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (node-handler id)
      (node-tasks-handler id))))

(defn form-general-grid [network]
  (comp/grid
    {:item true
     :key  "ngg"
     :xs   12}
    (rum/with-key
      (form-general network) "nggfg")))

(defn form-plugins-grid [networks volumes]
  (comp/grid
    {:item true
     :key  "npg"
     :xs   12}
    (rum/with-key
      (form-plugins networks volumes) "npgfg")))

(defn form-labels-grid [labels id]
  (comp/grid
    {:item true
     :key  "nlg"
     :xs   12}
    (rum/with-key
      (form-labels labels id) "nlgfg")))

(defn form-task-grid [tasks]
  (comp/grid
    {:item true
     :key  "nlg"
     :xs   12}
    (rum/with-key
      (form-tasks tasks) "ntgfg")))

(rum/defc form-info < rum/static [id {:keys [node tasks]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :key  "slg"
               :sm   6
               :md   6
               :lg   4}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-general-grid node)
                (form-plugins-grid
                  (->> node :plugins :networks)
                  (->> node :plugins :volumes))
                (form-labels-grid (:labels node) id)))
            (comp/grid
              {:item true
               :key  "srg"
               :sm   6
               :md   6
               :lg   8}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-task-grid tasks)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid node)
            (form-task-grid tasks)
            (form-plugins-grid
              (->> node :plugins :networks)
              (->> node :plugins :volumes))
            (form-labels-grid (:labels node) id)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [id]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info id item))))
