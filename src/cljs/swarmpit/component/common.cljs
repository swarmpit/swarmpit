(ns swarmpit.component.common
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn list-empty [title]
  (comp/card-content
    {:key "scclcc"}
    (comp/typography
      {:key "scclcct"} (str "There are no " title " configured."))))

(defn list-no-items-found []
  (comp/card-content
    {:key "scclcci"}
    (comp/typography
      {:key "scclccit"} "Nothing matches this filter.")))

(defn list
  [title items filtered-items render-metadata onclick-handler]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (cond
          (empty? items) (list-empty title)
          (empty? filtered-items) (list-no-items-found)
          :else
          (comp/card
            {:className "Swarmpit-card"
             :key       "scclc"}
            (comp/card-content
              {:className "Swarmpit-table-card-content"
               :key       "scclcc"}
              (rum/with-key
                (list/responsive
                  render-metadata
                  filtered-items
                  onclick-handler)
                "scclccrl"))))]])))

(defn show-password-adornment [show-password]
  (comp/input-adornment
    {:position "end"}
    (comp/icon-button
      {:aria-label  "Toggle password visibility"
       :onClick     #(state/update-value [:showPassword] (not show-password) state/form-state-cursor)
       :onMouseDown (fn [event]
                      (.preventDefault event))}
      (if show-password
        icon/visibility
        icon/visibility-off))))
