(ns swarmpit.component.service.log
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def form-state-cursor (conj state/form-state-cursor :logs))

(def line-num (atom 0))
(def last-scroll (atom 0))
(def last-glow (atom 0))

(defonce logs (atom nil))

(def history-options
  {"15m" "15 minutes"
   "30m" "30 minutes"
   "60m" "1 hour"
   "4h"  "4 hours"
   "8h"  "8 hours"
   "12h" "12 hours"
   "24h" "24 hours"
   0     "all"})

(defn- auto-scroll!
  []
  (when (and (:autoscroll (state/get-value form-state-cursor))
             (not (= @line-num @last-scroll)))
    (let [el (.getElementById js/document "swarmpit-log-autoscroll")]
      (.scrollTo el 0 (.-scrollHeight el))
      (reset! last-scroll @line-num))))

(defn form-search-fn
  [e]
  (state/update-value [:filter :predicate] (-> e .-target .-value) form-state-cursor))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:line %) predicate) items))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:service] response form-state-cursor))}))

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
    {:state      [:logs :fetching]
     :params     (when since {:since since})
     :on-success (fn [{:keys [origin? response]}]
                   (when origin?
                     (let [log-data (filter-logs task-id response)]
                       (reset! last-glow (count log-data))
                       (reset! logs log-data)
                       (state/update-value [:initialized] true form-state-cursor))))
     :on-error   (fn [{:keys [origin?]}]
                   (when origin?
                     (state/update-value [:error] true form-state-cursor)))}))

(defn- log-append-handler
  [service-id task-id since]
  (ajax/get
    (routes/path-for-backend :service-logs {:id service-id})
    {:state      [:logs :fetching]
     :params     (if since
                   {:since (-> since (time/to-unix) (inc))}
                   {:since "1m"})
     :on-success (fn [{:keys [origin? response]}]
                   (when origin?
                     (reset!
                       logs
                       (-> @logs
                           (concat (filter-logs task-id response))))))}))

(defn- init-form-state
  []
  (reset! last-glow 0)
  (reset! last-scroll 0)
  (reset! line-num 0)
  (state/set-value {:filter             {:predicate ""}
                    :mobileSearchOpened false
                    :history            "15m"
                    :initialized        false
                    :fetching           false
                    :autoscroll         false
                    :error              false
                    :timestamp          false} form-state-cursor))

(defn- init-form-value
  []
  (reset! logs nil))

(def mixin-refresh-form
  (mixin/refresh-form
    (fn [{:keys [id taskId]}]
      (let [state (state/get-value form-state-cursor)]
        (when (not (:fetching state))
          (log-append-handler id
                              taskId
                              (-> @logs
                                  (last)
                                  :timestamp)))))))

(rum/defc line < rum/static [item index show-taskId]
  [:div
   (when show-taskId
     [:a.log-info {:href (routes/path-for-frontend :task-info
                                                   {:id (:task item)}
                                                   {:log 1})}
      (str (subs (:task item) 0 5) " ")])
   [:span {:class (if (< @last-glow index)
                    (do (swap! last-glow inc)
                        "log-body Swarmpit-log-fresh")
                    "log-body")}
    (if (empty? (:line item))
      (str " ")
      (str (:line item)))]])

(rum/defc form-history < rum/reactive [history id taskId]
  (let [anchorEl (state/react (conj form-state-cursor :historyAnchorEl))]
    (html
      [:div
       [:div.Swarmpit-log-history-fab
        (comp/fab
          {:variant "extended"
           :onClick #(state/update-value [:historyAnchorEl] (.-currentTarget %) form-state-cursor)}
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
          :onClose         #(state/update-value [:historyAnchorEl] nil form-state-cursor)}
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
                            (state/update-value [:initialized] false form-state-cursor)
                            (log-handler id taskId k)
                            (state/update-value [:history] k form-state-cursor)
                            (state/update-value [:historyAnchorEl] nil form-state-cursor))}
               v)) history-options))])))

(rum/defc form-scroll < rum/static [autoscroll]
  (html
    [:div.Swarmpit-log-fab
     (comp/fab
       {:color   (if autoscroll "primary")
        :mini    true
        :onClick (fn []
                   (reset! last-scroll 0)
                   (state/update-value [:autoscroll] (not autoscroll) form-state-cursor))}
       (icon/scroll-down))]))

(rum/defc form-logs < rum/static [{:keys [initialized error]} logs filtered-logs show-taskId]
  (html
    [:div
     (cond
       error [:span "Logs for this service couldn't be fetched."]
       (and (empty? logs) initialized) [:span "No logs for given period."]
       (not initialized) [:span ""]
       :else (->> filtered-logs
                  (take-last 500)
                  (map #(rum/with-key (line % (:key %) show-taskId) (:key %)))))]))

(rum/defc form < rum/reactive
                 mixin-refresh-form
                 {:init       (fn [state _]
                                (let [{:keys [id taskId]} (first (:rum/args state))]
                                  (init-form-state)
                                  (init-form-value)
                                  (log-handler id taskId "15m"))
                                state)
                  :did-mount  (fn [state] (auto-scroll!) state)
                  :did-update (fn [state] (auto-scroll!) state)} [{:keys [id taskId]}]
  (let [{:keys [filter autoscroll history] :as log-state} (state/react form-state-cursor)
        log-data (rum/react logs)
        filtered-logs (filter-items log-data (:predicate filter))
        show-taskId (nil? taskId)]
    (html
      [:div.Swarmpit-log
       [:div.Swarmpit-toolbar]
       [:div
        (form-history history id taskId)
        (form-scroll autoscroll)
        (form-logs log-state log-data filtered-logs show-taskId)]])))

(rum/defc search-input < rum/static [filter]
  (html
    [:div.Swarmpit-appbar-search
     [:div.Swarmpit-appbar-search-icon (icon/search {})]
     (comp/input
       {:placeholder      (str "Search logs ...")
        :onChange         #(state/update-value [:filter :predicate] (-> % .-target .-value) form-state-cursor)
        :defaultValue     (:predicate filter)
        :fullWidth        true
        :classes          {:root  "Swarmpit-appbar-search-root"
                           :input "Swarmpit-appbar-search-input"}
        :id               "Swarmpit-logs-search-filter"
        :key              "logs-search"
        :disableUnderline true})]))

(rum/defc mobile-search-message < rum/static [filter]
  (html
    [:span#snackbar-mobile-search.Swarmpit-appbar-search-mobile-message
     (comp/input
       {:placeholder      (str "Search logs ...")
        :onChange         #(state/update-value [:filter :predicate] (-> % .-target .-value) form-state-cursor)
        :defaultValue     (:predicate filter)
        :fullWidth        true
        :classes          {:root  "Swarmpit-appbar-search-mobile-root"
                           :input "Swarmpit-appbar-search-mobile-input"}
        :disableUnderline true})]))

(rum/defc mobile-search-action < rum/static []
  (html
    [:span
     (comp/icon-button
       {:onClick    #(state/update-value [:mobileSearchOpened] false form-state-cursor)
        :key        "search-btn-close"
        :aria-label "Close"
        :color      "inherit"}
       (icon/close
         {:className "Swarmpit-appbar-search-mobile-close"}))]))

(rum/defc mobile-search < rum/static [opened filter]
  (comp/snackbar
    {:open         opened
     :anchorOrigin {:vertical   "top"
                    :horizontal "center"}
     :className    "Swarmpit-appbar-search-mobile"
     :onClose      #(state/update-value [:mobileSearchOpened] false form-state-cursor)}
    (comp/snackbar-content
      {:aria-describedby "snackbar-mobile-search"
       :className        "Swarmpit-appbar-search-mobile-content"
       :classes          {:message "Swarmpit-appbar-search-mobile-content-message"}
       :message          (mobile-search-message filter)
       :action           (mobile-search-action)})))

(rum/defc dialog < rum/reactive [serviceId taskId open?]
  (let [{:keys [initialized error filter mobileSearchOpened]} (state/react form-state-cursor)]
    (comp/dialog
      {:fullScreen      true
       :open            open?
       :PaperProps      {:id "swarmpit-log-autoscroll"}
       :aria-labelledby "form-full-dialog-title"}
      (comp/app-bar
        {:color     "primary"
         :className "Swarmpit-log-appbar"}
        (comp/toolbar
          {:disableGutters false}
          (comp/icon-button
            {:color   "inherit"
             :onClick #(dispatch!
                         (if taskId
                           (routes/path-for-frontend :task-info {:id taskId})
                           (routes/path-for-frontend :service-info {:id serviceId})))}
            (icon/close {}))
          (comp/typography
            {:variant   "h6"
             :className "Swarmpit-log-title"
             :color     "inherit"
             :noWrap    true}
            (if taskId
              "Task"
              "Service"))
          (comp/typography
            {:variant "h6"
             :color   "inherit"
             :noWrap  true}
            (if taskId
              (str serviceId "." (subs taskId 0 5))
              serviceId))
          (html [:div.grow])
          (comp/box
            {:className "Swarmpit-appbar-section-desktop"}
            (search-input filter))
          (comp/box
            {:className "Swarmpit-appbar-section-mobile"}
            (comp/icon-button
              {:key           "menu-search"
               :aria-haspopup "true"
               :onClick       #(state/update-value [:mobileSearchOpened] true form-state-cursor)
               :color         "inherit"} (icon/search {})))))
      (mobile-search mobileSearchOpened filter)
      (when (and (not initialized)
                 (false? error))
        (comp/linear-progress {:className "Swarmpit-log-progress"}))
      (form {:id     serviceId
             :taskId taskId}))))
