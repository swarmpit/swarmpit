(ns swarmpit.component.network.create
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(def cursor [:page :network :form])

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:networkName] v cursor))})))

(defn- form-driver [value]
  (comp/form-comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v cursor))}
      (comp/menu-item
        {:key         "fdi1"
         :value       "overlay"
         :primaryText "overlay"}))))

(defn- form-internal [value]
  (comp/form-comp
    "IS PRIVATE"
    (comp/form-checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:internal] v cursor))})))

(defn- form-subnet [value]
  (comp/form-comp
    "SUBNET"
    (comp/vtext-field
      {:name            "subnet"
       :key             "subnet"
       :validations     "isValidSubnet"
       :validationError "Please provide a valid CIDR format"
       :hintText        "e.g. 10.0.0.0/24"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:ipam :subnet] v cursor))})))

(defn- form-gateway [value]
  (comp/form-comp
    "GATEWAY"
    (comp/vtext-field
      {:name            "gateway"
       :key             "gateway"
       :validations     "isValidGateway"
       :validationError "Please provide a valid IP format"
       :hintText        "e.g. 10.0.0.1"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:ipam :gateway] v cursor))})))

(defn- create-network-info-msg
  [id]
  (str "Network " id " has been created."))

(defn- create-network-error-msg
  [error]
  (str "Network creation failed. Reason: " error))

(defn- create-network-handler
  []
  (ajax/POST (routes/path-for-backend :network-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "Id")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :network-info {:id id}))
                                 (message/mount!
                                   (create-network-info-msg id))))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")]
                                 (progress/unmount!)
                                 (message/mount!
                                   (create-network-error-msg error) true)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                driver
                internal
                ipam
                isValid
                isValidIpam]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/networks "New network")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (or (not isValid)
                            (not isValidIpam))
            :primary    true
            :onTouchTap create-network-handler}))]]

     [:div.form-view
      [:div.form-view-group
       (comp/form-section "General settings")
       (comp/form
         {:onValid   #(state/update-value [:isValid] true cursor)
          :onInvalid #(state/update-value [:isValid] false cursor)}
         (form-name name)
         (form-driver driver)
         (form-internal internal))]
      [:div.form-view-group
       (comp/form-section "IP address management")
       (comp/form
         {:onValid   #(state/update-value [:isValidIpam] true cursor)
          :onInvalid #(state/update-value [:isValidIpam] false cursor)}
         (form-subnet (:subnet ipam))
         (form-gateway (:gateway ipam)))]]]))

(defn- init-state
  []
  (state/set-value {:networkName nil
                    :driver      "overlay"
                    :internal    false
                    :ipam        nil
                    :isValid     false
                    :isValidIpam true} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))