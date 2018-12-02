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
    (let [el (.getElementById js/document "service-log")]
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

(defn- log-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:fetching]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:initialized] true state/form-state-cursor)
                   (state/set-value response state/form-value-cursor))
     :on-error   #(state/update-value [:error] true state/form-state-cursor)}))

(defn- log-append-handler
  [service-id from-timestamp]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:fetching]
     :params     {:from from-timestamp}
     :on-success (fn [{:keys [response]}]
                   (state/set-value (-> (state/get-value state/form-value-cursor)
                                        (concat response)) state/form-value-cursor))}))

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
    (fn [{{:keys [id]} :params}]
      (when (not (:fetching (state/get-value state/form-state-cursor)))
        (log-append-handler id (-> (state/get-value state/form-value-cursor)
                                   (last)
                                   :timestamp))))))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (service-handler id)
      (log-handler id))))

(rum/defc line < rum/static [item timestamp]
  [:div
   ;;   (when timestamp
   ;;   [:span.log-timestamp (:timestamp item)])
   [:span.log-info (str (subs (:task item) 0 12))]
   [:span.log-body (str " " (:line item))]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin-refresh-form
                 {:did-mount  (fn [state] (auto-scroll!) state)
                  :did-update (fn [state] (auto-scroll!) state)} [{{:keys [id]} :params}]
  (let [{:keys [filter autoscroll timestamp initialized error service]} (state/react state/form-state-cursor)
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
         [:div.Swarmpit-log
          (comp/grid
            {:container true
             :spacing   0}
            (comp/grid
              {:item true
               :xs   12}
              (html
                [:div#service-log
                 (cond
                   error [:span "Logs for this service couldn't be fetched."]
                   (and (empty? logs) initialized) [:span "Log is empty in this service."]
                   (not initialized) [:span "Loading..."]
                   :else (map
                           (fn [item]
                             (line item timestamp)) filtered-logs))])))]]))))
