(ns swarmpit.component.service.info.logdriver
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info :as list]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Name"
    :primary true
    :key     [:name]}
   {:name "Value"
    :key  [:value]}])

(rum/defc form < rum/static [{:keys [name opts]}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :subheader (form/subheader "Log driver options" icon/settings)})
    (comp/card-content
      {}
      (list/table
        render-metadata
        opts
        nil))))
