(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.parser :refer [parse-int]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :deployment))

(defn- form-restart-policy-attempts [value]
  (form/comp
    "MAX ATTEMPTS"
    (comp/vtext-field
      {:name     "restart-policy-attempts"
       :key      "restart-policy-attempts"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:restartPolicy :attempts] (parse-int v) form-value-cursor))})))

(defn- form-restart-policy-delay [value]
  (form/comp
    "DELAY"
    (comp/vtext-field
      {:name     "restart-policy-delay"
       :key      "restart-policy-delay"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:restartPolicy :delay] (parse-int v) form-value-cursor))})))

(defn- form-restart-policy-window [value]
  (form/comp
    "WINDOW"
    (comp/vtext-field
      {:name     "restart-policy-window"
       :key      "restart-policy-window"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:restartPolicy :window] (parse-int v) form-value-cursor))})))

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
    (comp/vtext-field
      {:name     "update-parallelism"
       :key      "update-parallelism"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:update :parallelism] (parse-int v) form-value-cursor))})))

(defn- form-update-delay [value]
  (form/comp
    "DELAY"
    (comp/vtext-field
      {:name     "update-delay"
       :key      "update-delay"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:update :delay] (parse-int v) form-value-cursor))})))

(defn- form-update-order [value]
  (form/comp
    "ORDER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:update :order] v form-value-cursor))}
      (comp/menu-item
        {:key         "fuo1"
         :value       "start-first"
         :primaryText "start-first"})
      (comp/menu-item
        {:key         "fuo2"
         :value       "stop-first"
         :primaryText "stop-first"}))))

(defn- form-update-failure-action [value]
  (form/comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:update :failureAction] v form-value-cursor))}
      (comp/menu-item
        {:key         "fufa1"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "fufa2"
         :value       "continue"
         :primaryText "continue"})
      (comp/menu-item
        {:key         "fufa3"
         :value       "rollback"
         :primaryText "rollback"}))))

(defn- form-rollback-parallelism [value]
  (form/comp
    "PARALLELISM"
    (comp/vtext-field
      {:name     "rollback-parallelism"
       :key      "rollback-parallelism"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:rollback :parallelism] (parse-int v) form-value-cursor))})))

(defn- form-rollback-delay [value]
  (form/comp
    "DELAY"
    (comp/vtext-field
      {:name     "rollback-delay"
       :key      "rollback-delay"
       :type     "number"
       :min      0
       :value    (str value)
       :onChange (fn [_ v]
                   (state/update-value [:rollback :delay] (parse-int v) form-value-cursor))})))

(defn- form-rollback-order [value]
  (form/comp
    "ORDER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:rollback :order] v form-value-cursor))}
      (comp/menu-item
        {:key         "fro1"
         :value       "stop-first"
         :primaryText "stop-first"})
      (comp/menu-item
        {:key         "fro2"
         :value       "start-first"
         :primaryText "start-first"}))))

(defn- form-rollback-failure-action [value]
  (form/comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:rollback :failureAction] v form-value-cursor))}
      (comp/menu-item
        {:key         "frfa1"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "frfa2"
         :value       "continue"
         :primaryText "continue"}))))

(defn- form-autoredeploy [value]
  (form/comp
    "AUTOREDEPLOY"
    (form/toogle
      {:name     "autoredeploy"
       :key      "autoredeploy"
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
       (form-restart-policy-window (:window restartPolicy))
       (form-restart-policy-attempts (:attempts restartPolicy))
       (html (form/subsection "Update Config"))
       (form-update-parallelism (:parallelism update))
       (form-update-delay (:delay update))
       (form-update-order (:order update))
       (form-update-failure-action (:failureAction update))
       (when (= "rollback" (:failureAction update))
         (html
           [:div
            (form/subsection "Rollback Config")
            (form-rollback-parallelism (:parallelism rollback))
            (form-rollback-delay (:delay rollback))
            (form-rollback-order (:order rollback))
            (form-rollback-failure-action (:failureAction rollback))])))]))