(ns swarmpit.component.stack.create-compose
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [material.component.panel :as panel]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(def cursor [:form])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Description"
               :width "50%"}])

(defonce searching? (atom false))

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- stacks-handler
  []
  (handler/get
    (routes/path-for-backend :stack-files)
    {:state      searching?
     :on-success (fn [response]
                   (state/update-value [:files] response cursor))
     :on-error   (fn [response]
                   (message/error
                     (str "Compose files fetching failed. Reason: " (:error response))))}))

(defn- form-compose [file]
  (comp/mui
    (form/comp
      "COMPOSE"
      (comp/text-field
        {:hintText "Find compose file"
         :value    file
         :onChange (fn [_ v]
                     (state/update-value [:files] v cursor)
                     (stacks-handler))}))))

(defn- init-state
  []
  (state/set-value {:files []
                    :file  ""} cursor))

(def mixin-init-form
  (mixin/init-form-tab
    (fn []
      (init-state))))

(rum/defc form-list < rum/static [searching? files]
  (let [compose (fn [index] (:name (nth files index)))]
    [:div.form-edit-loader
     (if searching?
       (progress/loading)
       (progress/loaded))
     (comp/mui
       (comp/table
         {:key         "tbl"
          :selectable  false
          :onCellClick (fn [i]
                         (dispatch!
                           (routes/path-for-frontend :stack-create-config {} {:compose (compose i)})))}
         (list/table-header headers)
         (list/table-body headers
                          files
                          render-item
                          [[:name]])))]))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [file files]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/stacks "New stack")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :stack-create-config)
            :label   "New stack"
            :primary true}))]]
     [:div.form-edit
      (form-compose file)
      (form-list (rum/react searching?) files)]]))

