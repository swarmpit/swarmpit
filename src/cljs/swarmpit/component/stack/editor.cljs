(ns swarmpit.component.stack.editor
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [cljsjs.codemirror]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :stack :form])

(defn editor
  []
  (js/CodeMirror.fromTextArea
    (.getElementById js/document "editor")
    (clj->js
      {:lineNumbers       true
       :viewportMargin    (.-Infinity js/window)
       :matchBrackets     true
       ;:value             "test"
       :autofocus         true
       :autoCloseBrackets true
       :mode              "yaml"})))

(defn- form-name [value]
  (comp/form-comp
    "STACK NAME"
    (comp/vtext-field
      {:name     "stack-name"
       :key      "stack-name"
       :required true
       :value    value})))

(defn- form-editor []
  (comp/mui
    (comp/text-field
      {:id            "editor"
       :name          "data"
       :key           "data"
       :multiLine     true
       :rows          10
       :underlineShow false
       :fullWidth     true
       :onChange      (fn [_ v]
                        (print v))})))

(rum/defc form < {:did-mount (fn [state] (editor) state)}
                 rum/static [_]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/stacks "New stack")]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:containerElement "label"
          :label            "Import stack"}
         (html [:input#tf {:type     "file"
                           :onChange (fn [value]
                                       (print (-> value
                                                  .-target
                                                  .-value)))
                           :style    {:display "none"}}])))


     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:label   "Create"
          ;:onTouchTap (fn [] (js/console.log (-> (editor) .getValue)))
          :primary true}))]]
   (comp/form
     {:onValid   #(state/update-value [:isValid] true cursor)
      :onInvalid #(state/update-value [:isValid] false cursor)}
     (form-name "test"))
   (form-editor)])
