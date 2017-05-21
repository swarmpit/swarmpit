(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :deployment])

(defn- form-autoredeploy [value]
  (comp/form-edit-row
    "AUTOREDEPLOY"
    (comp/toogle
      {:toggled  value
       :onToggle (fn [e v] (state/update-value :autoredeploy v cursor))
       :style    {:marginTop "14px"}})))

(rum/defc form < rum/reactive []
  (let [{:keys [autoredeploy]} (state/react cursor)]
    [:div.form-edit
     (form-autoredeploy autoredeploy)]))
