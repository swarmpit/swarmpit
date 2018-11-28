(ns swarmpit.component.service.form-deployment-placement
  (:require [swarmpit.component.state :as state]
            [material.component.composite :as composite]
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
  (let [suggestions (map #(hash-map :label %
                                    :value %) placement-list)]
    (composite/autocomplete
      {:options        suggestions
       :textFieldProps {:label           "Placement"
                        :margin          "normal"
                        :helperText      "Speficy placement constraints"
                        :InputLabelProps {:shrink true}}
       :onChange       (fn [value]
                         (state/set-value
                           (->> (js->clj value)
                                (map #(hash-map :rule (get % "value")))) form-value-cursor))
       :value          (map #(hash-map :label (:rule %)
                                       :value (:rule %)) placement)
       :placeholder    "Add placement"
       :isMulti        true})))

(rum/defc form < rum/reactive []
  (let [{:keys [list]} (state/react form-state-cursor)
        placement (state/react form-value-cursor)]
    (form-placement placement list)))
