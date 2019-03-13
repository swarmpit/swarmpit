(ns swarmpit.component.accounts
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def render-metadata
  {:primary   (fn [item] (:username item))
   :secondary (fn [item] (:secret-access-key item))})

(rum/defc form < rum/static [secrets service-id]
  (list/list
    render-metadata
    secrets
    nil))

(rum/defc form < rum/reactive []

  )