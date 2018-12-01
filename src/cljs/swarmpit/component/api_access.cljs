(ns swarmpit.component.api-access
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
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

(defn- form-token [value]
  (comp/text-field
    {:label           "Authorization Token"
     :fullWidth       true
     :name            "token"
     :key             "token"
     :variant         "outlined"
     :margin          "normal"
     :multiline       true
     :value           value
     :InputProps      {:readOnly true
                       :style    {:fontFamily "monospace"}}
     :InputLabelProps {:shrink true}}))

(rum/defc form-api-token < rum/static [{:keys [api-token token]}]
  (let [state (if (and api-token (not token))
                :old (if token
                       :new :none))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (comp/card
              {:className "Swarmpit-form-card"}
              (comp/card-header
                {:className "Swarmpit-form-card-header"
                 :title     (case state :none "Create API token" :old "API Token" :new "New API token")})
              (comp/card-content
                {}
                (comp/grid
                  {:container true
                   :spacing   40}
                  (comp/grid
                    {:item true
                     :xs   12}
                    (case state
                      :old [(comp/typography {} ["Token for this user was already created, if you lost it, you can regenerate it and "
                                                 "the former token will be revoked."])
                            (form-token (str "Bearer ..." (:mask api-token)))]
                      :new [(comp/typography {} ["Copy your generated token and store it safely, value will be displayed only once."])
                            (form-token (:token token))]
                      :none [(comp/typography {} "Your user doesn't have any API token.")
                             (comp/typography {} "You can create authorization token here. Generated token doesn't expire, but it can be revoked.")])))
                (html
                  [:div.Swarmpit-form-buttons
                   (comp/button
                     {:variant  "contained"
                      :disabled false
                      :onClick  generate-handler
                      :color    "primary"}
                     (case state :none "Generate" "Regenerate"))
                   (comp/button
                     {:variant  "contained"
                      :disabled (= :none state)
                      :onClick  remove-handler}
                     "Remove")]))))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-api-token item))))

