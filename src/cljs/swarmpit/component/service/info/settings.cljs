(ns swarmpit.component.service.info.settings
  (:require [material.component :as comp]
            [cljs-time.core]
            [rum.core :as rum]
            [clojure.string :as str]))

(enable-console-print!)

(defn service-name
  [service-name stack]
  (if (some? stack)
    (str/replace service-name #"^.*_" "")
    service-name))

(rum/defc form < rum/static [service]
  (let [image-digest (get-in service [:repository :imageDigest])
        stack (:stack service)]
    [:div.form-service-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id service))
     (if (some? stack)
       (comp/form-item "STACK" stack))
     (comp/form-item "NAME" (service-name (:serviceName service) stack))
     (comp/form-item-date "CREATED" (:createdAt service))
     (comp/form-item-date "LAST UPDATE" (:updatedAt service))
     (comp/form-item "IMAGE" (get-in service [:repository :image]))
     (when (some? image-digest)
       (comp/form-item "IMAGE DIGEST" image-digest))
     (comp/form-item "MODE" (:mode service))]))

