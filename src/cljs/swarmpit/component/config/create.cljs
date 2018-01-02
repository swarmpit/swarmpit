(ns swarmpit.component.config.create
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce isValid (atom false))

(def form-data-style
  {:padding  "10px"
   :border   "1px solid rgb(224, 224, 224)"
   :minWidth "400px"})

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:configName] v cursor))})))

(defn- form-data [value]
  (form/textarea
    "DATA"
    (comp/vtext-field
      {:name          "data"
       :key           "data"
       :required      true
       :multiLine     true
       :rows          10
       :fullWidth     true
       :textareaStyle form-data-style
       :value         value
       :onChange      (fn [_ v]
                        (state/update-value [:data] v cursor))})))

(defn- create-config-handler
  []
  (handler/post
    (routes/path-for-backend :config-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :config-info (select-keys response [:id])))
                   (message/info
                     (str "Config " (:id response) " has been created.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Config creation failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:configName nil
                    :data       ""} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [configName data]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/secrets "New config")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not (rum/react isValid))
            :primary    true
            :onTouchTap create-config-handler}))]]
     [:div.form-edit
      (form/form
        {:onValid   #(reset! isValid true)
         :onInvalid #(reset! isValid false)}
        (form-name configName)
        (form-data data))]]))