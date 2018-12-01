(ns swarmpit.component.common
  (:require [material.components :as comp]
            [material.component.list.basic :as list]
            [sablono.core :refer-macros [html]]
            [material.icon :as icon]
            [swarmpit.component.state :as state]))

(defn list
  [title items filtered-items render-metadata onclick-handler]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (cond
          (empty? items)
          (comp/card-content {} (comp/typography {} (str "There are no " title " configured.")))
          (empty? filtered-items)
          (comp/card-content {} (comp/typography {} "Nothing matches this filter."))
          :else
          (comp/card
            {:className "Swarmpit-card"}
            (comp/card-content
              {:className "Swarmpit-table-card-content"}
              (list/responsive
                render-metadata
                filtered-items
                onclick-handler))))]])))

(defn show-password-adornment [show-password]
  (comp/input-adornment
    {:position "end"}
    (comp/icon-button
      {:aria-label  "Toggle password visibility"
       :onClick     #(state/update-value [:showPassword] (not show-password) state/form-state-cursor)
       :onMouseDown (fn [event]
                      (.preventDefault event))}
      (if show-password
        icon/visibility-off
        icon/visibility))))
