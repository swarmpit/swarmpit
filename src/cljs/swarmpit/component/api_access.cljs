(ns swarmpit.component.api-access
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]))

(enable-console-print!)

(defn- me-handler
  []
  (ajax/get
    (routes/path-for-backend :me)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:api-token] (:api-token response) state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (me-handler))))

(defn- generate-handler
  []
  (ajax/post
    (routes/path-for-backend :api-token-generate)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:token] response state/form-value-cursor))
     :on-error   (fn [_]
                   (message/error (str "Failed to generate API token.")))}))

(defn- remove-handler
  []
  (ajax/delete
    (routes/path-for-backend :api-token-remove)
    {:on-success (fn [_]
                   (state/update-value [:api-token] nil state/form-value-cursor)
                   (state/update-value [:token] nil state/form-value-cursor))
     :on-error   (fn [_]
                   (message/error (str "Failed to remove API token.")))}))

;(defn- token-input
;  [value]
;  (form/form
;    nil
;    (form/comp
;      "AUTHORIZATION TOKEN"
;      (comp/vtext-field
;        {:name      "token"
;         :key       "token"
;         :multiLine true
;         :style     {:width      "450px"
;                     :fontFamily "monospace"}
;         :value     value}))))
;
;(rum/defc form-api-token < rum/static [{:keys [api-token token]}]
;  (let [state (if (and api-token (not token))
;                :old (if token
;                       :new :none))]
;    [:div
;     [:div.form-panel
;      [:div.form-panel-left
;       (panel/info icon/password (case state :none "Create API token" :old "API Token" :new "New API token"))]
;      [:div.form-panel-right
;       (comp/progress-button
;         {:label      (case state :none "Generate" "Regenerate")
;          :disabled   false
;          :primary    true
;          :onTouchTap generate-handler}
;         false)
;       [:span.form-panel-delimiter]
;       (comp/progress-button
;         {:label      "Remove"
;          :disabled   (= :none state)
;          :onTouchTap remove-handler}
;         false)]]
;     (case state
;       :old [(form/value ["Token for this user was already created, if you lost it, you can regenerate it and "
;                          "the former token will be revoked."])
;             (token-input (str "Bearer ..." (:mask api-token)))]
;       :new [(form/value ["Copy your generated token and store it safely, value will be displayed only once."])
;             (token-input (:token token))]
;       :none [(form/value "Your user doesn't have any API token.")
;              (form/value "You can create authorization token here. Generated token doesn't expire, but it can be revoked.")])]))
;
;(rum/defc form < rum/reactive
;                 mixin-init-form []
;  (let [state (state/react state/form-state-cursor)
;        item (state/react state/form-value-cursor)]
;    (progress/form
;      (:loading? state)
;      (form-api-token item))))

