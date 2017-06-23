(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :deployment])

(defn- form-parallelism [value]
  (comp/form-comp
    "PARALLELISM"
    (comp/text-field
      {:name     "parallelism"
       :key      "parallelism"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:parallelism] (js/parseInt v) cursor))})))

(defn- form-delay [value]
  (comp/form-comp
    "DELAY"
    (comp/text-field
      {:name     "delay"
       :key      "delay"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:delay] (js/parseInt v) cursor))})))

(defn- form-failure-action [value]
  (comp/form-comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:failureAction] v cursor))}
      (comp/menu-item
        {:key         "fdi1"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "fdi2"
         :value       "continue"
         :primaryText "continue"}))))

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
  (let [{:keys [autoredeploy
                parallelism
                delay
                failureAction]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {}
       (form-parallelism parallelism)
       (form-delay delay)
       (form-failure-action failureAction)
       (form-autoredeploy autoredeploy))]))