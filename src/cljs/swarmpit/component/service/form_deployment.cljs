(ns swarmpit.component.service.form-deployment
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :wizard :deployment])

(defn- form-update-parallelism [value]
  (comp/form-comp
    "PARALLELISM"
    (comp/text-field
      {:name     "update-parallelism"
       :key      "update-parallelism"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:update :parallelism] (js/parseInt v) cursor))})))

(defn- form-update-delay [value]
  (comp/form-comp
    "DELAY"
    (comp/text-field
      {:name     "update-delay"
       :key      "update-delay"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:update :delay] (js/parseInt v) cursor))})))

(defn- form-update-failure-action [value]
  (comp/form-comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:update :failureAction] v cursor))}
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
  (comp/form-comp
    "PARALLELISM"
    (comp/text-field
      {:name     "rollback-parallelism"
       :key      "rollback-parallelism"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:rollback :parallelism] (js/parseInt v) cursor))})))

(defn- form-rollback-delay [value]
  (comp/form-comp
    "DELAY"
    (comp/text-field
      {:name     "rollback-delay"
       :key      "rollback-delay"
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:rollback :delay] (js/parseInt v) cursor))})))

(defn- form-rollback-failure-action [value]
  (comp/form-comp
    "FAILURE ACTION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:rollback :failureAction] v cursor))}
      (comp/menu-item
        {:key         "fdir1"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "fdir2"
         :value       "continue"
         :primaryText "continue"}))))

(defn- form-autoredeploy [value]
  (comp/form-comp
    "AUTOREDEPLOY"
    (comp/form-toogle
      {:name     "autoredaploy"
       :key      "autoredaploy"
       :disabled (nil? value)
       :toggled  value
       :onToggle (fn [_ v]
                   (state/update-value [:autoredeploy] v cursor))})))

(rum/defc form < rum/reactive [placement]
  (let [{:keys [autoredeploy
                update
                rollback]} (state/react cursor)]
    [:div.form-edit
     (comp/form
       {}
       (form-autoredeploy autoredeploy)
       (html (comp/form-subsection "Update Config"))
       (form-update-parallelism (:parallelism update))
       (form-update-delay (:delay update))
       (form-update-failure-action (:failureAction update))
       (when (= "rollback" (:failureAction update))
         (html
           [:div
            (comp/form-subsection "Rollback Config")
            (form-rollback-parallelism (:parallelism rollback))
            (form-rollback-delay (:delay rollback))
            (form-rollback-failure-action (:failureAction rollback))]))
       (html (comp/form-subsection-add "Placement" placement/add-item))
       (placement/form placement))]))