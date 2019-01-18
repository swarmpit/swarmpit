(ns swarmpit.component.node.edit
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [material.component.list.edit :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-node-labels-cursor (conj state/form-value-cursor :labels))

(def doc-node-link "https://docs.docker.com/engine/swarm/manage-nodes/#list-nodes")

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :margin          "normal"
     :style           {:maxWidth "350px"}
     :defaultValue    value
     :required        true
     :disabled        true
     :InputLabelProps {:shrink true}}))

(defn- form-role [value]
  (comp/form-control
    {:component "fieldset"
     :key       "role-f"
     :style     {:width "200px"}}
    (comp/form-label
      {:key "rolel"} "Role")
    (comp/radio-group
      {:name     "role"
       :key      "role-rg"
       :value    value
       :onChange #(state/update-value [:role] (-> % .-target .-value) state/form-value-cursor)}
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "manager-role"
                     :color "primary"
                     :key   "manager-role"})
         :key     "mngr-role"
         :value   "manager"
         :label   "Manager"})
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "worker-role"
                     :color "primary"
                     :key   "worker-role"})
         :key     "worker-role"
         :value   "worker"
         :label   "Worker"}))))

(defn- form-availability [value]
  (comp/form-control
    {:component "fieldset"
     :key       "availability-f"
     :style     {:width "200px"}}
    (comp/form-label
      {:key "availabilityl"} "Availability")
    (comp/radio-group
      {:name     "availability"
       :key      "availability-rg"
       :value    value
       :onChange #(state/update-value [:availability] (-> % .-target .-value) state/form-value-cursor)}
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "active"
                     :color "primary"
                     :key   "active"})
         :key     "active-av"
         :value   "active"
         :label   "Active"})
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "pause"
                     :color "primary"
                     :key   "pause"})
         :key     "pause-av"
         :value   "pause"
         :label   "Pause"})
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "drain"
                     :color "primary"
                     :key   "drain"})
         :key     "drain-av"
         :value   "drain"
         :label   "Drain"}))))

(defn- form-label-name [value index]
  (comp/text-field
    {:fullWidth       true
     :placeholder     "Name"
     :key             (str "form-label-name-" index)
     :defaultValue    value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :name (-> % .-target .-value) form-node-labels-cursor)}))

(defn- form-label-value [value index]
  (comp/text-field
    {:fullWidth       true
     :placeholder     "Value"
     :key             (str "form-label-value-" index)
     :defaultValue    value
     :required        true
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :value (-> % .-target .-value) form-node-labels-cursor)}))

(def form-label-metadata
  [{:name      "Name"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-label-name value index))}
   {:name      "Value"
    :key       [:value]
    :render-fn (fn [value _ index] (form-label-value value index))}])

(defn- form-label-table
  [labels]
  (rum/with-key
    (list/list
      form-label-metadata
      labels
      (fn [index] (state/remove-item index form-node-labels-cursor))) "necccill"))

(defn- add-label
  []
  (state/add-item {:name  ""
                   :value ""} form-node-labels-cursor))

(defn- node-handler
  [node-id]
  (ajax/get
    (routes/path-for-backend :node {:id node-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-node-handler
  [node-id version]
  (ajax/post
    (routes/path-for-backend :node-update {:id node-id})
    {:params     (-> (state/get-value state/form-value-cursor)
                     (assoc :version version))
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :node-info {:id node-id})))
                   (message/info
                     (str "Node " node-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Node update failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      true
                    :loading?    true
                    :processing? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (node-handler id))))

(rum/defc form-edit < rum/static [{:keys [id version nodeName role availability labels]}
                                  {:keys [processing? valid?]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :key       "snoccg"
           :spacing   40}
          (comp/grid
            {:item true
             :key  "snooccgif"
             :xs   12
             :sm   12
             :md   12
             :lg   8
             :xl   8}
            (comp/card
              {:className "Swarmpit-form-card"
               :key       "nec"}
              (comp/card-header
                {:className "Swarmpit-form-card-header"
                 :key       "nech"
                 :title     (html [:span "Editing " [:span.Swarmpit-secondary-title nodeName]])})
              (comp/card-content
                {:key "necc"}
                (comp/grid
                  {:container true
                   :key       "neccc"
                   :spacing   40}
                  (comp/grid
                    {:item true
                     :key  "necccigar"
                     :xs   12
                     :sm   6}
                    (form-role role))
                  (comp/grid
                    {:item true
                     :key  "necccigaa"
                     :xs   12
                     :sm   6}
                    (form-availability availability))
                  (comp/grid
                    {:item true
                     :key  "necccil"
                     :xs   12}
                    (form/section
                      "Labels"
                      (comp/button
                        {:color   "primary"
                         :onClick add-label}
                        (comp/svg
                          {:key "necccilbtn"} icon/add-small-path) "Add label"))
                    (form-label-table labels)))
                (html
                  [:div {:class "Swarmpit-form-buttons"
                         :key   "neccbtn"}
                   (composite/progress-button
                     "Save"
                     #(update-node-handler id version)
                     processing?)]))))
          (comp/grid
            {:item true
             :key  "snoccgid"
             :xs   12
             :sm   12
             :md   12
             :lg   4
             :xl   4}
            (form/open-in-new "Learn more about nodes" doc-node-link)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/scroll-to-section [_]
  (let [state (state/react state/form-state-cursor)
        node (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit node state))))