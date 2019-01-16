(ns swarmpit.component.service.info.secrets
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :secret-info {:id (:secretName item)})))

(def render-metadata
  {:primary   (fn [item] (:secretName item))
   :secondary (fn [item] (:secretTarget item))})

(rum/defc form < rum/static [secrets service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "ssec"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "ssech"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "secrets-title"} "Secrets")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Secrets"})}
                    (comp/svg icon/edit-path))})
    (if (empty? secrets)
      (comp/card-content
        {:key "sseccrle"}
        (html [:div "No secrets defined for the service."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "ssecc"}
        (rum/with-key
          (list/list
            render-metadata
            secrets
            onclick-handler) "sseccrl")))))

