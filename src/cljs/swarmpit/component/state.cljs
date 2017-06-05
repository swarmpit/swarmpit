(ns swarmpit.component.state
  (:require [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(defonce state
         (atom {:menu {:opened true
                       :domain ""}
                :page {:network    {:list {:filter {}}
                                    :form {}}
                       :node       {:list {:filter {}}}
                       :service    {:list {:filter {}}
                                    :form {:settings   {}
                                           :deployment {}
                                           :ports      []
                                           :variables  []
                                           :volumes    []}}
                       :task       {:list {:filter {}}}
                       :registry   {:list   {:filter {}}
                                    :form   {}
                                    :wizard {}}
                       :user       {:list {:filter {}}}
                       :repository {:list {:data {}}}}}))

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
  [value cursor]
  (swap! state assoc-in cursor value))

(defn update-value
  "Update value `v` corresponding to key path `p` on given `cursor`"
  [p v cursor]
  (swap! state update-in cursor
         (fn [map] (assoc-in map p v))))

(defn add-item
  "Add `item` to vector on given `cursor`"
  [item cursor]
  (swap! state update-in cursor
         (fn [vec] (conj vec item))))

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