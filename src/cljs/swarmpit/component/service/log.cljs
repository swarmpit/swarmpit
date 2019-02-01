(ns swarmpit.component.service.log
  (:require [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn- auto-scroll!
  []
  (when (true? (:autoscroll (state/get-value state/form-state-cursor)))
    (let [el (.getElementById js/document "autoscroll")]
      (.scrollTo js/window 0 (.-scrollHeight el)))))

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


(def line-num (atom 0))

(defn- filter-logs
  [taskId log]
  (->> log
       (filter-by-task taskId)
       (map #(assoc-in % [:key] (swap! line-num inc)))))

(defn- log-handler
  [service-id task-id]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:fetching]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:initialized] true state/form-state-cursor)
                   (state/set-value (filter-logs task-id response) state/form-value-cursor))
     :on-error   #(state/update-value [:error] true state/form-state-cursor)}))

(defn- log-append-handler
  [service-id task-id from-timestamp]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:fetching]
     :params     {:from from-timestamp}
     :on-success (fn [{:keys [response]}]
                   (state/set-value (-> (state/get-value state/form-value-cursor)
                                        (concat (filter-logs task-id response))) state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:filter      {:predicate ""}
                    :initialized false
                    :fetching    false
                    :autoscroll  false
                    :error       false
                    :timestamp   false} state/form-state-cursor))

(def mixin-refresh-form
  (mixin/refresh-form
    (fn [{{:keys [id taskId]} :params}]
      (when (not (:fetching (state/get-value state/form-state-cursor)))
        (log-append-handler id taskId (-> (state/get-value state/form-value-cursor)
                                          (last)
                                          :timestamp))))))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id taskId]} :params}]
      (init-form-state)
      (service-handler id)
      (log-handler id taskId))))

(rum/defc line < rum/static [service item]
  [:div
   [:a.log-info {:href (routes/path-for-frontend :service-task-log {:id (:serviceName service) :taskId (:task item)})}
    (str (subs (:task item) 0 7))]
   [:span.log-body (str " " (:line item))]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin-refresh-form
                 {:did-mount  (fn [state] (auto-scroll!) state)
                  :did-update (fn [state] (auto-scroll!) state)} [{{:keys [id]} :params}]
  (let [{:keys [filter autoscroll initialized error service]} (state/react state/form-state-cursor)
        logs (state/react state/form-value-cursor)
        filtered-logs (filter-items logs (:predicate filter))]
    (comp/mui
      (html
        [:div
         [:div.Swarmpit-log-fab
          (comp/button
            {:variant "fab"
             :color   (if autoscroll "primary")
             :mini    true
             :onClick #(state/update-value [:autoscroll] (not autoscroll) state/form-state-cursor)}
            icon/scroll-down)]
         [:div#autoscroll.Swarmpit-log
          (cond
            error [:span "Logs for this service couldn't be fetched."]
            (and (empty? logs) initialized) [:span "Log is empty in this service."]
            (not initialized) [:span "Loading..."]
            :else (->> filtered-logs
                       (take-last 500)
                       (map #(rum/with-key (line service %) (:key %)))))]]))))

(rum/defc form-task < form [params]
  (form params))
