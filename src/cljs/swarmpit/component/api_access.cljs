(ns swarmpit.component.api-access
  (:require [material.components :as comp]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- generate-handler
  []
  (ajax/post
    (routes/path-for-backend :api-token)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:token] response state/form-value-cursor))
     :on-error   (fn [_]
                   (message/error (str "Failed to generate API token.")))}))

(defn- remove-handler
  []
  (ajax/delete
    (routes/path-for-backend :api-token)
    {:on-success (fn [_]
                   (state/update-value [:api-token] nil state/form-value-cursor)
                   (state/update-value [:token] nil state/form-value-cursor))
     :on-error   (fn [_]
                   (message/error (str "Failed to remove API token.")))}))

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

(defn- format-expiry [iso]
  (try
    (.toLocaleString (js/Date. iso))
    (catch :default _ iso)))

(defn- expiry-line [api-token]
  (comp/typography
    {:key       "expiry"
     :className "Swarmpit-fcard-message"
     :variant   "caption"}
    (if-let [exp (:expiresAt api-token)]
      (str "Expires: " (format-expiry exp))
      "This token does not expire.")))

(defn old-token-form [api-token]
  [(comp/typography {:key       "info"
                     :className "Swarmpit-fcard-message"
                     :variant   "body2"}
                    ["Token for this user was already created. If you lost your token, please generate new one and "
                     "the former token will be revoked."])
   (form-token (str "Bearer ..." (:mask api-token)))
   (expiry-line api-token)])

(defn new-token-form [token]
  [(comp/typography {:key       "notice"
                     :className "Swarmpit-fcard-message"
                     :variant   "body2"} "Copy your token and store it safely, value will be displayed only once.")
   (form-token (:token token))
   (expiry-line token)])

(defn no-token-form []
  [(comp/typography {:key       "notoken"
                     :className "Swarmpit-fcard-message"
                     :variant   "body2"}
                    "Your user doesn't have any API token. A new token can be revoked or regenerated at any time.")])

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [{:keys [loading?]} (state/react state/form-state-cursor)
        {:keys [api-token token]} (state/react state/form-value-cursor)
        state (if (and api-token (not token))
                :old
                (if token :new :none))]
    (if loading?
      (comp/linear-progress {:className "Swarmpit-progress"})
      (comp/paper
        {:className "Swarmpit-form-card"
         :elevation 0}
        (comp/card-content
          {}
          (case state
            :old (old-token-form api-token)
            :new (new-token-form token)
            :none (no-token-form)))
        (comp/card-actions
          {:className "Swarmpit-fcard-actions"}
          (comp/button
            {:variant  "contained"
             :key      "submit"
             :disabled false
             :onClick  generate-handler
             :color    "primary"}
            (case state :none "Generate" "Regenerate"))
          (comp/button
            {:variant  "outlined"
             :key      "remove"
             :disabled (= :none state)
             :onClick  remove-handler}
            "Remove"))))))
