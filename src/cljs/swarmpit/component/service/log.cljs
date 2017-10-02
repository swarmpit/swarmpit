(ns swarmpit.component.service.log
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def cursor [:page :service :log])

(defn auto-scroll!
  []
  (when (true? (:autoscroll (state/get-value cursor)))
    (let [el (.getElementById js/document "service-log")]
      (set! (.-scrollTop el)
            (.-scrollHeight el)))))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:line %) predicate) items))

(defn log-handler
  [service]
  (handler/get
    (routes/path-for-backend :service-logs (select-keys service [:id]))
    {:on-call    (state/update-value [:fetching] true cursor)
     :on-success (fn [response]
                   (state/update-value [:initialized] true cursor)
                   (state/update-value [:fetching] false cursor)
                   (state/update-value [:data] response cursor))
     :on-error   #(state/update-value [:error] true cursor)}))

(defn log-append-handler
  [service from-timestamp]
  (handler/get
    (routes/path-for-backend :service-logs (select-keys service [:id]))
    {:on-call    (state/update-value [:fetching] true cursor)
     :params     {:from from-timestamp}
     :on-success (fn [response]
                   (state/update-value [:fetching] false cursor)
                   (state/update-value [:data] (-> (state/get-value cursor)
                                                   :data
                                                   (concat response)) cursor))}))

(defn- init-state
  []
  (state/set-value {:filter      {:predicate ""}
                    :initialized false
                    :fetching    false
                    :autoscroll  false
                    :error       false
                    :timestamp   false
                    :data        []} cursor))

(def refresh-state-mixin
  (mixin/refresh
    (fn [service]
      (when (not (:fetching (state/get-value cursor)))
        (log-append-handler service (-> (state/get-value cursor)
                                        :data
                                        (last)
                                        :timestamp))))))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc line < rum/static [item timestamp]
  [:div
   (when timestamp
     [:span.log-timestamp (:timestamp item)])
   [:span.log-info (str (:taskName item) "." (subs (:task item) 0 12) "@" (:taskNode item))]
   [:span.log-body (str " " (:line item))]])

(rum/defc form < rum/reactive
                 init-state-mixin
                 refresh-state-mixin
                 {:did-mount  (fn [state] (auto-scroll!) state)
                  :did-update (fn [state] (auto-scroll!) state)} [service]
  (let [{:keys [filter data autoscroll timestamp initialized error]} (state/react cursor)
        filtered-items (filter-items data
                                     (:predicate filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName service))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href  (routes/path-for-frontend :service-info (select-keys service [:id]))
            :label "Back"}))]]
     [:div.log-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Search in log"
          :onChange (fn [_ v]
                      (state/update-value [:filter :predicate] v cursor))})
       [:span.form-panel-space]
       (comp/panel-checkbox
         {:checked timestamp
          :label   "Show timestamp"
          :onCheck (fn [_ v]
                     (state/update-value [:timestamp] v cursor))})]
      [:div.form-panel-right
       (comp/panel-checkbox
         {:checked autoscroll
          :label   "Auto-scroll logs"
          :onCheck (fn [_ v]
                     (state/update-value [:autoscroll] v cursor))})]]
     [:div.log#service-log
      (cond
        error [:span "Logs for this service couldn't be fetched."]
        (and (empty? filtered-items) initialized) [:span "Log is empty in this service."]
        (not initialized) [:span "Loading..."]
        :else (map
                (fn [item]
                  (line item timestamp)) filtered-items))]]))
