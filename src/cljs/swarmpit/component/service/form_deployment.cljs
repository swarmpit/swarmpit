(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :deployment))

(defn- form-restart-policy-attempts [value]
  (form/comp
    "MAX ATTEMPTS"
    (comp/text-field
      {:name     "restart-policy-attempts"
       :key      "restart-policy-attempts"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:restartPolicy :attempts] (js/parseInt v) form-value-cursor))})))

(defn- form-restart-policy-delay [value]
  (form/comp
    "DELAY"
    (comp/text-field
      {:name     "restart-policy-delay"
       :key      "restart-policy-delay"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:restartPolicy :delay] (js/parseInt v) form-value-cursor))})))

(defn- form-restart-policy-condition [value]
  (form/comp
    "CONDITION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:restartPolicy :condition] v form-value-cursor))}
      (comp/menu-item
        {:key         "frpc1"
         :value       "any"
         :primaryText "any"})
      (comp/menu-item
        {:key         "frpc2"
         :value       "on-failure"
         :primaryText "on-failure"})
      (comp/menu-item
        {:key         "frpc3"
         :value       "none"
         :primaryText "none"}))))

(defn- form-update-parallelism [value]
  (form/comp
    "PARALLELISM"
    (comp/text-field
      {:name     "update-parallelism"
       :key      "update-parallelism"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:update :parallelism] (js/parseInt v) form-value-cursor))})))

(defn- form-update-delay [value]
  (form/comp
    "DELAY"
    (comp/text-field
      {:name     "update-delay"
       :key      "update-delay"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:update :delay] (js/parseInt v) form-value-cursor))})))

(defn- form-update-failure-action [value]
  (form/comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:update :failureAction] v form-value-cursor))}
      (comp/menu-item
        {:key         "fdiu1"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "fdiu2"
         :value       "continue"
         :primaryText "continue"})
      (comp/menu-item
        {:key         "fdiu3"
         :value       "rollback"
         :primaryText "rollback"}))))

(defn- form-rollback-parallelism [value]
  (form/comp
    "PARALLELISM"
    (comp/text-field
      {:name     "rollback-parallelism"
       :key      "rollback-parallelism"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:rollback :parallelism] (js/parseInt v) form-value-cursor))})))

(defn- form-rollback-delay [value]
  (form/comp
    "DELAY"
    (comp/text-field
      {:name     "rollback-delay"
       :key      "rollback-delay"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:rollback :delay] (js/parseInt v) form-value-cursor))})))

(defn- form-rollback-failure-action [value]
  (form/comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:rollback :failureAction] v form-value-cursor))}
      (comp/menu-item
        {:key         "fdir1"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "fdir2"
         :value       "continue"
         :primaryText "continue"}))))

(defn- form-autoredeploy [value]
  (form/comp
    "AUTOREDEPLOY"
    (form/toogle
      {:name     "autoredeploy"
       :key      "autoredeploy"
       :disabled (nil? value)
       :toggled  value
       :onToggle (fn [_ v]
                   (state/update-value [:autoredeploy] v form-value-cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [autoredeploy
                update
                rollback
                restartPolicy]} (state/react form-value-cursor)]
    [:div.form-edit
     (form/form
       {}
       (form-autoredeploy autoredeploy)
       (html (form/subsection-add "Placement" placement/add-item))
       (placement/form)
       (html (form/subsection "Restart Policy"))
       (form-restart-policy-condition (:condition restartPolicy))
       (form-restart-policy-delay (:delay restartPolicy))
       (form-restart-policy-attempts (:attempts restartPolicy))
       (html (form/subsection "Update Config"))
       (form-update-parallelism (:parallelism update))
       (form-update-delay (:delay update))
       (form-update-failure-action (:failureAction update))
       (when (= "rollback" (:failureAction update))
         (html
           [:div
            (form/subsection "Rollback Config")
            (form-rollback-parallelism (:parallelism rollback))
            (form-rollback-delay (:delay rollback))
            (form-rollback-failure-action (:failureAction rollback))])))]))