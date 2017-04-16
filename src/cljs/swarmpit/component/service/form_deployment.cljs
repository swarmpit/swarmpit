(ns swarmpit.component.service.form-deployment
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {}))

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-autoredeploy [value]
  (material/form-edit-row
    "AUTOREDEPLOY"
    (material/toogle
      #js {:toggled  value
           :onToggle (fn [e v] (update-item :autoredeploy v))
           :style    #js {:marginTop "14px"}})))

(rum/defc form < rum/reactive []
  (let [{:keys [autoredeploy]} (rum/react state)]
    [:div.form-edit
     (form-autoredeploy autoredeploy)]))
