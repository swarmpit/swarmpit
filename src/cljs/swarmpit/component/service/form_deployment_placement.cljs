(ns swarmpit.component.service.form-deployment-placement
  (:require [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (into state/form-value-cursor [:deployment :placement]))

(def form-state-cursor (conj state/form-state-cursor :placement))

(defn placement-handler
  []
  (ajax/get
    (routes/path-for-backend :placement)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:list] response form-state-cursor))}))

(defn- form-placement [placement placement-list]
  (let [placements (map #(hash-map :rule %) placement-list)]
    (comp/autocomplete
      {:id                    "placement-autocomplete"
       :multiple              true
       :filterSelectedOptions true
       :options               placements
       :value                 placement
       :getOptionLabel        (fn [option] (goog.object/get option "rule"))
       :renderInput           (fn [params]
                                (comp/text-field-js
                                  (js/Object.assign
                                    params
                                    #js {:label      "Placement"
                                         :fullWidth  true
                                         :margin     "normal"
                                         :variant    "outlined"
                                         :helperText "Specify placement constraints"})))
       :onChange              (fn [e v]
                                (state/set-value
                                  (->> (js->clj v)
                                       (map #(hash-map :rule (get % "rule")))) form-value-cursor))})))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        placement (state/react form-value-cursor)]
    (form-placement placement list)))