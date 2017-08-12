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

(defn- data-handler
  [service]
  (handler/get
    (routes/path-for-backend :service-logs (select-keys service [:id]))
    {:on-success (fn [response]
                   (state/update-value [:data] response cursor))}))

(defn- init-state
  [logs]
  (state/set-value {:filter     {:predicate ""}
                    :autoscroll true
                    :timestamp  false
                    :data       logs} cursor))

(def refresh-state-mixin
  (mixin/refresh
    (fn [data]
      (data-handler (:service data)))))

(def init-state-mixin
  (mixin/init
    (fn [data]
      (init-state (:logs data)))))

(rum/defc form < rum/reactive
                 init-state-mixin
                 refresh-state-mixin
                 {:did-mount  (fn [state] (auto-scroll!) state)
                  :did-update (fn [state] (auto-scroll!) state)} [{:keys [service]}]
  (let [{:keys [filter data autoscroll timestamp]} (state/react cursor)
        filtered-items (filter-items data
                                     (:predicate filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName service))]]

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
      (if (empty? filtered-items)
        [:span "There are no logs available for the service."]
        (for [item filtered-items]
          [:span
           (when timestamp
             [:span.log-timestamp (:timestamp item)])
           [:span.log-info (str (:taskName item) "." (subs (:task item) 0 12) "@" (:taskNode item))]
           [:span.log-body (str " " (:line item))]
           [:br]]))]]))
