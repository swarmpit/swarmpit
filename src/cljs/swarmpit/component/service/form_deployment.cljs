(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :form :deployment])

(def form-autoredeploy-style
  {:marginTop "14px"})

(defn- form-autoredeploy [value]
  (comp/form-comp
    "AUTOREDEPLOY"
    (comp/toogle
      {:toggled  value
       :style    form-autoredeploy-style
       :onToggle (fn [_ v]
                   (state/update-value :autoredeploy v cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [autoredeploy]} (state/react cursor)]
    [:div.form-edit
     (form-autoredeploy autoredeploy)]))
