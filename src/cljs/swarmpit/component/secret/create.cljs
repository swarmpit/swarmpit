(ns swarmpit.component.secret.create
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

(def cursor [:page :secret :form])

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
                   (state/update-value [:secretName] v cursor))})))

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

(defn- form-data-encoder [value]
  (form/comp
    "ENCODE DATA"
    (form/checkbox
      {:name    "encoded"
       :key     "encoded"
       :checked value
       :onCheck (fn [_ v]
                  (state/update-value [:encode] v cursor))})))

(defn- create-secret-handler
  []
  (handler/post
    (routes/path-for-backend :secret-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :secret-info (select-keys response [:id])))
                   (message/info
                     (str "Secret " (:id response) " has been created.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Secret creation failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:secretName nil
                    :data       ""
                    :encode     false
                    :isValid    false} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin []
  (let [{:keys [secretName
                data
                encode
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/secrets "New secret")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-secret-handler}))]]

     [:div.form-view
      [:div.form-view-group
       (form/icon-value icon/info "Data must be base64 encoded. If plain text check please encode data.")
       (form/form
         {:onValid   #(state/update-value [:isValid] true cursor)
          :onInvalid #(state/update-value [:isValid] false cursor)}
         (form-name secretName)
         (form-data-encoder encode)
         (form-data data))]]]))