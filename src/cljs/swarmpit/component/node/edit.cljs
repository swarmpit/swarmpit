(ns swarmpit.component.node.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
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

(def form-node-labels-headers
  [{:name  "Name"
    :width "35%"}
   {:name  "Value"
    :width "35%"}])

(defn- form-role [value]
  (form/comp
    "ROLE"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:role] v state/form-value-cursor))}
      (comp/menu-item
        {:key         "frm"
         :value       "manager"
         :primaryText "manager"})
      (comp/menu-item
        {:key         "frw"
         :value       "worker"
         :primaryText "worker"}))))

(defn- form-availability [value]
  (form/comp
    "AVAILABILITY"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:availability] v state/form-value-cursor))}
      (comp/menu-item
        {:key         "faa"
         :value       "active"
         :primaryText "active"})
      (comp/menu-item
        {:key         "faw"
         :value       "pause"
         :primaryText "pause"})
      (comp/menu-item
        {:key         "fad"
         :value       "drain"
         :primaryText "drain"}))))

(defn- form-labels-name [value index]
  (list/textfield
    {:name     (str "form-labels-name-" index)
     :key      (str "form-labels-name-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :name v form-node-labels-cursor))}))

(defn- form-labels-value [value index]
  (list/textfield
    {:name     (str "form-labels-value-" index)
     :key      (str "form-labels-value-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v form-node-labels-cursor))}))

(defn- form-labels-render
  [item index]
  (let [{:keys [name value]} item]
    [(form-labels-name name index)
     (form-labels-value value index)]))

(defn- form-labels-table
  [labels]
  (list/table-raw form-node-labels-headers
                  labels
                  nil
                  form-labels-render
                  (fn [index] (state/remove-item index form-node-labels-cursor))))

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
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/nodes nodeName)]
    [:div.form-panel-right
     (comp/progress-button
       {:label      "Save"
        :disabled   (not valid?)
        :primary    true
        :onTouchTap #(update-node-handler id version)} processing?)
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :node-info {:id id})
          :label "Back"}))]]
   [:div.form-layout
    [:div.form-layout-group
     (form/subsection "General settings")
     (form/form
       {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
        :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
       (form-role role)
       (form-availability availability))]
    [:div.form-layout-group.form-layout-group-border
     (form/subsection "Labels")
     (form/form
       {}
       (html (form/subsection-add "Add label" add-label))
       (form-labels-table labels))]]])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        node (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit node state))))