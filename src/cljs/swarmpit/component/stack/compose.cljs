(ns swarmpit.component.stack.compose
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.compose-diff :as diff]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "compose")

(def doc-compose-link "https://docs.docker.com/compose/how-tos/getting-started/")

(defn form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :disabled        true
     :margin          "normal"
     :InputLabelProps {:shrink true}}))

(defn- humanize-kind [kind]
  (case kind
    "stack-create"     "Created"
    "stack-update"     "Updated"
    "stack-redeploy"   "Redeployed"
    "stack-rollback"   "Rolled back"
    "service-update"   "Service updated"
    "service-redeploy" "Service redeployed"
    "service-rollback" "Service rolled back"
    "service-scale"    "Service scaled"
    "auto-redeploy"    "Auto-redeploy"
    (or kind "Change")))

(defn- relative-time [iso]
  (try
    (let [past (.getTime (js/Date. iso))
          now  (.getTime (js/Date.))
          s    (max 0 (quot (- now past) 1000))]
      (cond
        (js/isNaN past) "Unknown time"
        (< s 60)       "just now"
        (< s 3600)     (str (quot s 60) "m ago")
        (< s 86400)    (str (quot s 3600) "h ago")
        (< s 2592000)  (str (quot s 86400) "d ago")
        (< s 31536000) (str (quot s 2592000) "mo ago")
        :else          (str (quot s 31536000) "y ago")))
    (catch :default _ iso)))

(defn- history-label [{:keys [at by trigger comment]}]
  (let [{:keys [kind service]} trigger]
    (html
      [:div.Swarmpit-compose-history
       {:title (str (humanize-kind kind) " · " at " · " by
                    (when service (str " · " service))
                    (when (seq comment) (str " · " comment)))}
       [:div.Swarmpit-compose-history-title
        (or (not-empty comment)
            (str (humanize-kind kind) (when service (str " · " service))))]
       [:div.Swarmpit-compose-history-meta
        (str (relative-time at) " · " (or by "system"))]])))

(defn- clear-diff-gutters! [editor]
  (let [n (.lineCount editor)]
    (dotimes [i n]
      (.setGutterMarker editor i "cm-diff-gutter" nil))
    (aset editor "__diffDeletions" (array))))

(defn- restore-deletion! [editor entry]
  (when-let [line (.getLineNumber editor (aget entry "handle"))]
    (let [current (vec (for [i (range (.lineCount editor))] (.getLine editor i)))
          {:keys [from to text]} (diff/restoration current
                                   {:b-start (+ line (aget entry "startOffset"))
                                    :b-end   (+ line (aget entry "endOffset"))
                                    :lines   (aget entry "lines")})]
      (.replaceRange editor text (clj->js from) (clj->js to)))))

(defn- restore-line! [editor line]
  (.operation editor
              (fn []
                (doseq [entry (reverse (filter #(= line (.getLineNumber editor (aget % "handle")))
                                              (aget editor "__diffDeletions")))]
                  (restore-deletion! editor entry)))))

(defn- ensure-gutter-click! [editor]
  (when-not (aget editor "__diffGutterBound")
    (aset editor "__diffGutterBound" true)
    (.on editor "gutterClick"
         (fn [cm line gutter _event]
           (when (= gutter "cm-diff-gutter")
             (restore-line! cm line))))))

(defn- deletion-marker [editor handle lines]
  (let [node (.createElement js/document "div")
        joined (clojure.string/join "\n" lines)]
    (set! (.-className node) "cm-diff-deleted-marker")
    (set! (.-textContent node) "−")
    (.setAttribute node "data-tooltip" joined)
    (.setAttribute node "data-count" (count lines))
    (.setAttribute node "role" "button")
    (.setAttribute node "tabindex" "0")
    (.setAttribute node "aria-label" (str "Restore " (count lines) " lines from current state"))
    (.addEventListener node "keydown"
                       (fn [event]
                         (when (contains? #{"Enter" " "} (.-key event))
                           (.preventDefault event)
                           (when-let [line (.getLineNumber editor handle)]
                             (restore-line! editor line)))))
    node))

(defn- apply-diff!
  [editor baseline]
  (when editor
    (ensure-gutter-click! editor)
    (let [n (.lineCount editor)]
      (dotimes [i n]
        (.removeLineClass editor i "background" "cm-diff-changed"))
      (clear-diff-gutters! editor)
      (when (and baseline (not (empty? baseline)))
        (let [cur   (vec (for [i (range n)] (.getLine editor i)))
              ;; -1 keeps the trailing empty string, which CodeMirror also has
              ;; as its last line — split-lines drops it and every diff then
              ;; reports a phantom change on the final line
              base  (vec (clojure.string/split baseline #"\n" -1))
              {:keys [added chunks]} (diff/changes base cur)
              store (aget editor "__diffDeletions")]
          (doseq [i added]
            (.addLineClass editor i "background" "cm-diff-changed"))
          (doseq [[anchor changes] (group-by #(max 0 (min (dec n) (dec (:b-end %)))) chunks)]
            (let [handle (.getLineHandle editor anchor)]
              (doseq [{:keys [b-start b-end lines]} changes]
                (.push store #js {:handle      handle
                                 :startOffset (- b-start anchor)
                                 :endOffset   (- b-end anchor)
                                 :lines       lines}))
              (.setGutterMarker editor anchor "cm-diff-gutter"
                                (deletion-marker editor handle (mapcat :lines changes))))))))))

(defn refresh-diff!
  []
  (let [{:keys [editor baseline]} (state/get-value state/form-state-cursor)]
    (when editor
      (js/clearTimeout (aget editor "__diffTimer"))
      (aset editor "__diffTimer"
            (js/setTimeout
              (fn []
                (let [signature [(.getValue editor) baseline]]
                  (when (not= signature (aget editor "__diffLast"))
                    (.operation editor #(apply-diff! editor baseline))
                    (aset editor "__diffLast" signature))))
              200)))))

(defn- load-compose! [source compose]
  (when-let [editor (state/get-value (conj state/form-state-cursor :editor))]
    (when (some? compose)
      (state/update-value [:source] source state/form-state-cursor)
      (.setValue editor compose))))

(defn- load-history-entry! [idx history]
  (let [entry (nth history idx nil)
        compose (get-in entry [:spec :compose])]
    (load-compose! (str "history-" idx) compose)))

(defn- ->str [x]
  (cond
    (nil? x) ""
    (keyword? x) (cljs.core/name x)
    :else (str x)))

(defn form-select
  ([name value last? previous?] (form-select name value last? previous? nil nil))
  ([name value last? previous? history] (form-select name value last? previous? history nil))
  ([name value last? previous? history source]
   (let [current (let [s (->str source)]
                   (if (empty? s) (->str value) s))]
     (comp/text-field
       {:fullWidth       true
        :key             "compose-select"
        :label           "Compose file"
        :select          true
        :value           current
        :variant         "outlined"
        :margin          "normal"
        :InputLabelProps {:shrink true}
        :SelectProps     {:MenuProps {:classes {:paper "Swarmpit-compose-source-menu"}}}
        :onChange        (fn [e]
                           (let [v (-> e .-target .-value)]
                             (cond
                               (clojure.string/starts-with? v "history-")
                               (load-history-entry! (js/parseInt (subs v 8)) history)

                               (= v (->str value))
                               (load-compose! v (state/get-value (conj state/form-state-cursor :source-compose)))

                               :else (dispatch! (routes/path-for-frontend (keyword v) {:name name})))))}
       (comp/menu-item {:key "current"  :value "stack-compose"}  "Current engine state")
       (comp/menu-item {:key "last"     :value "stack-last"
                        :disabled (not last?)} "Last deployed")
       (comp/menu-item {:key "previous" :value "stack-previous"
                        :disabled (not previous?)} "Previously deployed")
       (when (seq history)
         (comp/menu-item {:key "history-header" :value "history-header"
                          :disabled true} "History"))
       (map-indexed
         (fn [idx entry]
           (comp/menu-item {:key (str "history-" idx) :value (str "history-" idx)}
                           (history-label entry)))
         history)))))

(defn- form-editor [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
     :className       "Swarmpit-codemirror"
     :name            "config-view"
     :key             "config-view"
     :multiline       true
     :disabled        true
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :value           value}))

(defn- form-skip-image-resolve [value]
  (comp/form-control-label
    {:control (comp/checkbox
                {:checked  value
                 :onChange #(state/update-value [:skipImageResolve] (-> % .-target .-checked) state/form-value-cursor)})
     :label   "without image resolution"}))

(defn- update-stack-handler
  [name]
  (ajax/post
    (routes/path-for-backend :stack {:name name})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :stack-info {:name name})))
                   (message/info
                     (str "Stack " name " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack update failed. " (:error response))))}))

(defn- compose-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-compose {:name name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/set-value response state/form-value-cursor)
                     (state/update-value [:source-compose] (get-in response [:spec :compose]) state/form-state-cursor)
                     (state/update-value [:baseline]
                                         (get-in response [:spec :compose])
                                         state/form-state-cursor)
                     (refresh-diff!)))}))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.setOption editor "gutters" #js ["CodeMirror-lint-markers" "cm-diff-gutter"])
       (state/update-value [:editor] editor state/form-state-cursor)
       (.on editor "change"
            (fn [cm]
              (aset cm "__diffLast" nil)
              (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor)
              (refresh-diff!)))
       (refresh-diff!)
       (assoc state ::editor editor)))
   :will-unmount
   (fn [state]
     (when-let [editor (::editor state)]
       (js/clearTimeout (aget editor "__diffTimer"))
       (.toTextArea ^js editor))
     state)})

(defn stackfile-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-file {:name name})
    {:on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (when (:spec response) (state/update-value [:last?] true state/form-state-cursor))
                     (when (:previousSpec response) (state/update-value [:previous?] true state/form-state-cursor))))
     :on-error   (fn [{:keys [origin?]}]
                   (when origin?
                     (state/update-value [:last?] false state/form-state-cursor)
                     (state/update-value [:previous?] false state/form-state-cursor)))}))

(defn history-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-history {:name name})
    {:on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:history] (vec (reverse response)) state/form-state-cursor)))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :last?       false
                    :previous?   false
                    :history     []
                    :editor      nil
                    :source      :stack-compose
                    :baseline    nil
                    :loading?    true
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  [name]
  (state/set-value {:name             name
                    :spec             {:compose ""}
                    :skipImageResolve false} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-state)
      (init-form-value name)
      (stackfile-handler name)
      (history-handler name)
      (compose-handler name))))

(rum/defc form-edit < rum/reactive
                      mixin-init-editor [{:keys [name spec skipImageResolve]}
                                         {:keys [processing? valid? last? previous? history source]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/container
          {:maxWidth  "md"
           :className "Swarmpit-container"}
          (comp/card
            {:className "Swarmpit-form-card Swarmpit-fcard"}
            (comp/box
              {:className "Swarmpit-fcard-header"}
              (comp/typography
                {:className "Swarmpit-fcard-header-title"
                 :variant   "h6"
                 :component "div"}
                "Edit stack"))
            (comp/card-content
              {:className "Swarmpit-fcard-content"}
              (form-name name)
              (form-select name :stack-compose last? previous? history source)
              (form-editor (:compose spec)))
            (comp/card-actions
              {:className "Swarmpit-fcard-actions"}
              (composite/progress-button
                "Deploy"
                #(update-stack-handler name)
                processing?
                false
                {:startIcon (comp/svg {} icon/rocket-path)})
              (form-skip-image-resolve skipImageResolve))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile state))))
