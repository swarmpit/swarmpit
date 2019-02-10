(ns swarmpit.component.state
  (:require [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(defonce state
         (atom {:route   nil
                :layout  {:mobileOpened       false
                          :mobileSearchOpened false
                          :menuAnchorEl       nil
                          :mobileMoreAnchorEl nil}
                :dialog  {:open false}
                :search  {:query ""}
                :message {:text ""
                          :time nil
                          :type :info
                          :open false}
                :form    {:id    nil
                          :state nil
                          :value nil}}))

(defn react
  [cursor]
  (-> (rum/cursor-in state cursor)
      (rum/react)))

(defn get-value
  "Get value on given `cursor`"
  [cursor]
  (get-in @state cursor))

(defn set-value
  "Set `value` on given `cursor`"
  ([value cursor]
   (swap! state assoc-in cursor value))
  ([value]
   (swap! state merge value)))

(defn update-value
  "Update value `v` corresponding to key path `p` on given `cursor`"
  [p v cursor]
  (swap! state update-in cursor
         (fn [map] (assoc-in map p v))))

(defn add-item
  "Add `item` to vector on given `cursor` and assoc key"
  [item cursor]
  (let [item-wk (assoc item :key (str (random-uuid)))]
    (swap! state update-in cursor
           (fn [vec] (conj vec item-wk)))))

(defn remove-item
  "Remove vector item corresponding to `index` on given `cursor`"
  [index cursor]
  (swap! state update-in cursor
         (fn [vec] (remove-el vec index))))

(defn update-item
  "Update vector item value corresponding to `index` & key `k` on given `cursor`"
  [index k v cursor]
  (swap! state update-in cursor
         (fn [vec] (assoc-in vec [index k] v))))

;; Form domain

(defn form-id
  "Get current form id"
  []
  (get-value [:form :id]))

(defn form-origin?
  "Check whether form origin."
  [origin-form-id]
  (= (form-id) origin-form-id))

(defn reset-form
  "Reset state form data"
  []
  (set-value {:id      (str (random-uuid))
              :classes nil
              :state   nil
              :value   nil} [:form]))

;; Key generator

(defn assoc-keys [coll]
  (into [] (map #(assoc % :key (str (random-uuid))) coll)))

;; Common cursors

(def form-value-cursor [:form :value])

(def form-state-cursor [:form :state])

(def docker-api-cursor [:docker :api])

(def layout-cursor [:layout])

(def search-cursor [:search])

(def route-cursor [:route])