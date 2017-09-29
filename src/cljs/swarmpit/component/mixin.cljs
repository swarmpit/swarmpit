(ns swarmpit.component.mixin
  (:require [rum.core :as rum]))

(defn refresh
  [handler]
  {:did-mount    (fn [state]
                   (let [comp (:rum/react-component state)
                         callback #(do (handler (first (:rum/args state)))
                                       (rum/request-render comp))
                         interval (js/setInterval callback 2000)]
                     (assoc state ::interval interval)))
   :will-unmount (fn [state]
                   (js/clearInterval (::interval state))
                   (dissoc state ::interval))})

(defn init
  [handler]
  {:init (fn [state _]
           (handler (first (:rum/args state)))
           state)})

(def focus-filter
  {:did-mount (fn [state]
                (-> js/document
                    (.getElementById "filter")
                    (.focus))
                state)})