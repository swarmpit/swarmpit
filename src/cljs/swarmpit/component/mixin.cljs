(ns swarmpit.component.mixin
  (:require [rum.core :as rum]
            [swarmpit.event.source :as event]
            [swarmpit.component.state :as state]))

(defn refresh-form
  ([handler] (refresh-form handler 2000))
  ([handler ms]
   {:did-mount    (fn [state]
                    (let [comp (:rum/react-component state)
                          callback #(do (handler (first (:rum/args state)))
                                        (rum/request-render comp))
                          interval (js/setInterval callback ms)]
                      (assoc state ::interval interval)))
    :will-unmount (fn [state]
                    (js/clearInterval (::interval state))
                    (dissoc state ::interval))}))

(defn init-form
  [handler]
  {:init         (fn [state _]
                   (handler (first (:rum/args state)))
                   state)
   :will-unmount (fn [state]
                   (state/reset-form)
                   state)})

(defn init-form-tab
  [handler]
  {:init (fn [state _]
           (handler (first (:rum/args state)))
           state)})

(def subscribe-form
  {:did-mount    (fn [state]
                   (event/open! (first (:rum/args state)))
                   state)
   :will-unmount (fn [state]
                   (event/close!)
                   state)})

(def focus-filter
  {:did-mount (fn [state]
                (-> js/document
                    (.getElementById "filter")
                    (.focus))
                state)})