(ns swarmpit.component.dashboard
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.label :as label]
            [material.component.form :as form]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.component.plot :as plot]
            [swarmpit.event.source :as event]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.humanize :as humanize]
            [rum.core :as rum]))

(def mixin-init-form
  (mixin/init-form
    (fn [route]
      )))

(rum/defc form-info < rum/reactive [item]
  (let []
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/hidden
            {:xsDown         true
             :implementation "js"}
            (comp/grid
              {:container true
               :spacing   16}
              (comp/grid
                {:item true
                 :sm   6
                 :md   4}
                (comp/grid
                  {:container true
                   :spacing   16}
                  ;(form-general-grid item)
                  ))
              (comp/grid
                {:item true
                 :sm   6
                 :md   8}
                (comp/grid
                  {:container true
                   :spacing   16}
                  ;(form-cpu-stats-grid stats-loading? stats-empty?)
                  ;(form-ram-stats-grid stats-loading? stats-empty?)
                  ))))
          (comp/hidden
            {:smUp           true
             :implementation "js"}
            (comp/grid
              {:container true
               :spacing   16}
              ;(form-general-grid item)
              ;(form-cpu-stats-grid stats-loading? stats-empty?)
              ;(form-ram-stats-grid stats-loading? stats-empty?)
              ))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [name]} :params}]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    ;(progress/form
    ;  (:loading? state)
    ;  (form-info name item))


    (html [:div "Bitch"])

    ))
