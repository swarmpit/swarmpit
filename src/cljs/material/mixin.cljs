(ns material.mixin
  (:require [rum.core :as rum]))

(defn list-refresh-mixin
  [handler]
  {:did-mount    (fn [state]
                   (let [comp (:rum/react-component state)
                         callback #(do (handler)
                                       (rum/request-render comp))
                         interval (js/setInterval callback 2000)]
                     (assoc state ::interval interval)))
   :will-unmount (fn [state]
                   (js/clearInterval (::interval state))
                   (dissoc state ::interval))})
