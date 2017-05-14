(ns swarmpit.component.service.form-deployment
  (:require [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :deployment])

(defn- form-autoredeploy [value]
  (material/form-edit-row
    "AUTOREDEPLOY"
    (material/toogle
      #js {:toggled  value
           :onToggle (fn [e v] (state/update-value :autoredeploy v cursor))
           :style    #js {:marginTop "14px"}})))

(rum/defc form < rum/reactive []
  (let [{:keys [autoredeploy]} (state/react cursor)]
    [:div.form-edit
     (form-autoredeploy autoredeploy)]))
