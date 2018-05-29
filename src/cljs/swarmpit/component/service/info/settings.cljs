(ns swarmpit.component.service.info.settings
  (:require [material.component.form :as form]
            [swarmpit.docker.utils :as utils]
            [rum.core :as rum]
            [clojure.string :as str]
            [swarmpit.routes :as routes]))

(enable-console-print!)

(rum/defc form < rum/static [service]
  (let [image-digest (get-in service [:repository :imageDigest])
        command (:command service)
        stack (:stack service)]
    [:div.form-layout-group
     (form/section "General settings")
     (form/item "ID" (:id service))
     (if (some? stack)
       (form/item "STACK" [:a {:href (routes/path-for-frontend :stack-info {:name stack})} stack]))
     (form/item "NAME" (utils/trim-stack stack (:serviceName service)))
     (form/item-date "CREATED" (:createdAt service))
     (form/item-date "LAST UPDATE" (:updatedAt service))
     (form/item "IMAGE" (get-in service [:repository :image]))
     (when (some? image-digest)
       (form/item "IMAGE DIGEST" image-digest))
     (form/item "MODE" (:mode service))
     (when command
       (form/item "COMMAND" [:pre {:style {:fontSize "0.9em"
                                           :margin 0}}
                             (let [merged (str/join " " command)]
                               (if (< 100 (count merged))
                                 (str/join "\n" command)
                                 merged))]))]))