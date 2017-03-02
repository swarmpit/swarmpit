(ns swarmpit.component.service.info
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  (material/form-row "Test" "A"))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
