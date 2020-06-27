(ns swarmpit.component.mixin
  (:require [rum.core :as rum]
            [goog.dom :refer [getElement]]
            [goog.object :refer [get]]
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
  {:init      (fn [state _]
                (state/reset-form)
                (handler (first (:rum/args state)))
                state)
   :did-mount (fn [state]
                (.scrollTo js/window 0 0)
                state)})

(def subscribe-form
  {:did-mount    (fn [state]
                   (event/open! (first (:rum/args state)))
                   state)
   :will-unmount (fn [state]
                   (event/close!)
                   state)})

(def scroll-to-section
  {:did-mount
   (fn [state]
     (let [section (-> state :rum/args first :params :section)]
       (when section
         (state/update-value [:active] (js/parseInt section) state/form-state-cursor)))
     state)})

(defn resize [div-id callback]
  {:did-mount
   (fn [state]
     (.addEventListener
       js/window
       "resize"
       (fn []
         (let [el (getElement div-id)
               width (get el "offsetWidth")
               height (get el "offsetHeight")]
           (callback width height)))) state)})

(def focus-filter
  {:did-mount (fn [state]
                ;(-> js/document
                ;    (.getElementById "Swarmpit-appbar-search-filter")
                ;    (.focus))
                state)})