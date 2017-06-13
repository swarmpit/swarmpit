(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :deployment])

(defn- form-autoredeploy [value]
  (comp/form-comp
    "AUTOREDEPLOY"
    (comp/form-toogle
      {:name     "autoredaploy"
       :key      "autoredaploy"
       :toggled  value
       :onToggle (fn [_ v]
                   (state/update-value [:autoredeploy] v cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [autoredeploy]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {}
       (form-autoredeploy autoredeploy))]))
