(ns swarmpit.component.service.log
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def line-num (atom 0))
(def last-scroll (atom 0))
(def last-glow (atom 0))

(def history-options
  {"15m" "15 minutes"
   "30m" "30 minutes"
   "60m" "1 hour"
   "4h"  "4 hours"
   "8h"  "8 hours"
   "12h" "12 hours"
   "24h" "24 hours"
   nil   "all"})

(defn- auto-scroll!
  []
  (when (and (:autoscroll (state/get-value state/form-state-cursor))
             (not (= @line-num @last-scroll)))
    (let [el (.getElementById js/document "autoscroll")]
      (.scrollTo js/window 0 (.-scrollHeight el))
      (reset! last-scroll @line-num))))

(defn form-search-fn
  [e]
  (state/update-value [:filter :predicate] (-> e .-target .-value) state/form-state-cursor))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:line %) predicate) items))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:service] response state/form-state-cursor))}))

(defn- filter-by-task
  [taskId log]
  (if taskId
    (filter #(= taskId (:task %)) log)
    log))

(defn- filter-logs
  [taskId log]
  (->> log
       (filter-by-task taskId)
       (map #(assoc-in % [:key] (swap! line-num inc)))))

(defn- log-handler
  [service-id task-id since]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:fetching]
     :params     (when since {:since since})
     :on-success (fn [{:keys [origin? response]}]
                   (when origin?
                     (let [logs (filter-logs task-id response)]
                       (reset! last-glow (count logs))
                       (state/update-value [:initialized] true state/form-state-cursor)
                       (state/set-value logs state/form-value-cursor))))
     :on-error   (fn [{:keys [origin?]}]
                   (when origin?
                     (state/update-value [:error] true state/form-state-cursor)))}))

(defn- log-append-handler
  [service-id task-id since]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:fetching]
     :params     (if since
                   {:since (-> since (time/to-unix) (inc))}
                   {:since "1m"})
     :on-success (fn [{:keys [origin? response]}]
                   (when origin?
                     (state/set-value (-> (state/get-value state/form-value-cursor)
                                          (concat (filter-logs task-id response))) state/form-value-cursor)))}))

(defn- init-form-state
  []
  (reset! last-glow 0)
  (reset! last-scroll 0)
  (reset! line-num 0)
  (state/set-value {:filter      {:predicate ""}
                    :history     "15m"
                    :initialized false
                    :fetching    false
                    :autoscroll  false
                    :error       false
                    :timestamp   false} state/form-state-cursor))

(def mixin-refresh-form
  (mixin/refresh-form
    (fn [{{:keys [id taskId]} :params}]
      (let [state (state/get-value state/form-state-cursor)]
        (when (not (:fetching state))
          (log-append-handler id
                              taskId
                              (-> (state/get-value state/form-value-cursor)
                                  (last)
                                  :timestamp)))))))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id taskId]} :params}]
      (init-form-state)
      (service-handler id)
      (log-handler id taskId "15m"))))

(rum/defc line < rum/static [service item index]
  [:div
   [:a.log-info {:href (routes/path-for-frontend :service-task-log {:id     (:serviceName service)
                                                                    :taskId (:task item)})}
    (str (subs (:task item) 0 7))]
   [:span {:class (if (< @last-glow index)
                    (do (swap! last-glow inc)
                        "log-body Swarmpit-log-fresh")
                    "log-body")}
    (str " " (:line item))]])

(rum/defc form-history < rum/reactive [history id taskId]
  (let [anchorEl (state/react (conj state/form-state-cursor :historyAnchorEl))]
    (html
      [:div
       [:div.Swarmpit-log-history-fab
        (comp/button
          {:variant "extendedFab"
           :onClick #(state/update-value [:historyAnchorEl] (.-currentTarget %) state/form-state-cursor)}
          (icon/access-time {:className "Swarmpit-log-history-fab-ico"})
          (get history-options history))]
       (comp/menu
         {:id              "list-filter-menu"
          :anchorEl        anchorEl
          :anchorOrigin    {:vertical   "top"
                            :horizontal "right"}
          :transformOrigin {:vertical   "top"
                            :horizontal "right"}
          :open            (some? anchorEl)
          :onClose         #(state/update-value [:historyAnchorEl] nil state/form-state-cursor)}
         (comp/menu-item
           {:className "Swarmpit-menu-info"
            :disabled  true}
           (html [:span (str "Period of last")]))
         (map
           (fn [[k v]]
             (comp/menu-item
               {:key      (str "item-" v)
                :selected (= k history)
                :onClick  (fn [e]
                            (state/update-value [:initialized] false state/form-state-cursor)
                            (log-handler id taskId k)
                            (state/update-value [:history] k state/form-state-cursor)
                            (state/update-value [:historyAnchorEl] nil state/form-state-cursor))}
               v)) history-options))])))

(rum/defc form-scroll < rum/static [autoscroll]
  (html
    [:div.Swarmpit-log-fab
     (comp/button
       {:variant "fab"
        :color   (if autoscroll "primary")
        :mini    true
        :onClick (fn []
                   (reset! last-scroll 0)
                   (state/update-value [:autoscroll] (not autoscroll) state/form-state-cursor))}
       icon/scroll-down)]))

(rum/defc form-logs < rum/static [{:keys [initialized error service]} logs filtered-logs]
  (html
    [:div#autoscroll.Swarmpit-log
     (cond
       error [:span "Logs for this service couldn't be fetched."]
       (and (empty? logs) initialized) [:span "No logs for given period."]
       (not initialized) [:span "Loading..."]
       :else (->> filtered-logs
                  (take-last 500)
                  (map #(rum/with-key (line service % (:key %)) (:key %)))))]))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin-refresh-form
                 {:did-mount  (fn [state] (auto-scroll!) state)
                  :did-update (fn [state] (auto-scroll!) state)} [{{:keys [id taskId]} :params}]
  (let [{:keys [filter autoscroll history] :as log-state} (state/react state/form-state-cursor)
        logs (state/react state/form-value-cursor)
        filtered-logs (filter-items logs (:predicate filter))]
    (comp/mui
      (html
        [:div
         (form-history history id taskId)
         (form-scroll autoscroll)
         (form-logs log-state logs filtered-logs)]))))

(rum/defc form-task < form [params]
  (form params))
