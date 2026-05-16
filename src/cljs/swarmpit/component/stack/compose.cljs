(ns swarmpit.component.stack.compose
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
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
        (< s 60)       "just now"
        (< s 3600)     (str (quot s 60) "m ago")
        (< s 86400)    (str (quot s 3600) "h ago")
        (< s 2592000)  (str (quot s 86400) "d ago")
        (< s 31536000) (str (quot s 2592000) "mo ago")
        :else          (str (quot s 31536000) "y ago")))
    (catch :default _ iso)))

(defn- history-label [{:keys [at by trigger]}]
  (let [{:keys [kind service]} trigger]
    (str (humanize-kind kind)
         (when service (str " · " service))
         " · " (relative-time at)
         " · " by)))

(defn- lcs-script [a b]
  (let [m  (count a)
        nk (inc (count b))
        dp (reduce
             (fn [dp i]
               (conj dp (reduce
                          (fn [r j]
                            (conj r (if (= (nth a (dec i)) (nth b (dec j)))
                                      (inc (get-in dp [(dec i) (dec j)]))
                                      (max (get-in dp [(dec i) j]) (peek r)))))
                          [0] (range 1 nk))))
             [(vec (repeat nk 0))] (range 1 (inc m)))
        ops (loop [i m, j (dec nk), ops ()]
              (cond
                (and (pos? i) (pos? j) (= (nth a (dec i)) (nth b (dec j))))
                (recur (dec i) (dec j) (conj ops [:keep]))
                (and (pos? i) (or (zero? j) (>= (get-in dp [(dec i) j]) (get-in dp [i (dec j)]))))
                (recur (dec i) j (conj ops [:del (nth a (dec i))]))
                (pos? j) (recur i (dec j) (conj ops [:add]))
                :else ops))]
    (reduce
      (fn [{:keys [bi chunk-start chunk-lines] :as s} [op text]]
        (case op
          :keep (-> (if (seq chunk-lines)
                      (update s :chunks conj
                              {:b-start (or chunk-start bi)
                               :b-end   bi
                               :lines   chunk-lines})
                      s)
                    (assoc :chunk-start nil :chunk-lines [])
                    (update :bi inc))
          :add  (-> s
                    (update :added conj bi)
                    (assoc :chunk-start (or chunk-start bi))
                    (update :bi inc))
          :del  (-> s
                    (assoc :chunk-start (or chunk-start bi))
                    (update :chunk-lines conj text))))
      {:added #{} :chunks [] :bi 0 :chunk-start nil :chunk-lines []}
      (concat ops [[:keep]]))))

(defn- clear-diff-gutters! [editor]
  (let [n (.lineCount editor)]
    (dotimes [i n]
      (.setGutterMarker editor i "cm-diff-gutter" nil))
    (aset editor "__diffDeletions" (array))))

(defn- restore-deletion! [editor handle chunk-size lines]
  (let [line   (.getLineNumber editor handle)
        joined (clojure.string/join "\n" lines)]
    (if (pos? chunk-size)
      (.replaceRange editor
                     (str joined "\n")
                     #js {:line (- line (dec chunk-size)) :ch 0}
                     #js {:line (inc line) :ch 0})
      (.replaceRange editor
                     (str "\n" joined)
                     #js {:line line :ch (.-length (or (.getLine editor line) ""))}
                     #js {:line line :ch (.-length (or (.getLine editor line) ""))}))))

(defn- ensure-gutter-click! [editor]
  (when-not (aget editor "__diffGutterBound")
    (aset editor "__diffGutterBound" true)
    (.on editor "gutterClick"
         (fn [cm line gutter _event]
           (when (= gutter "cm-diff-gutter")
             (when-let [entry (some (fn [e]
                                      (when (= line (.getLineNumber cm (aget e "handle")))
                                        e))
                                    (aget cm "__diffDeletions"))]
               (restore-deletion! cm
                                  (aget entry "handle")
                                  (aget entry "chunkSize")
                                  (aget entry "lines"))))))))

(defn- deletion-marker [lines]
  (let [node (.createElement js/document "div")
        joined (clojure.string/join "\n" lines)]
    (set! (.-className node) "cm-diff-deleted-marker")
    (set! (.-textContent node) "−")
    (.setAttribute node "data-tooltip" joined)
    (.setAttribute node "data-count" (count lines))
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
              base  (clojure.string/split-lines baseline)
              {:keys [added chunks]} (lcs-script base cur)
              store (aget editor "__diffDeletions")]
          (doseq [i added]
            (.addLineClass editor i "background" "cm-diff-changed"))
          (doseq [{:keys [b-start b-end lines]} chunks]
            (let [size   (- b-end b-start)
                  anchor (max 0 (min (dec n) (if (pos? size) (dec b-end) (dec b-start))))
                  handle (.getLineHandle editor anchor)]
              (.push store #js {:handle    handle
                                :chunkSize size
                                :lines     (clj->js lines)})
              (.setGutterMarker editor anchor "cm-diff-gutter"
                                (deletion-marker lines)))))))))

(defn refresh-diff! []
  (let [state (state/get-value state/form-state-cursor)]
    (apply-diff! (:editor state) (:baseline state))))

(defn- load-history-entry! [idx history]
  (let [entry (nth history idx nil)
        editor (state/get-value (conj state/form-state-cursor :editor))
        compose (get-in entry [:spec :compose])]
    (when (and editor compose)
      (state/update-value [:source] (str "history-" idx) state/form-state-cursor)
      (.setValue editor compose))))

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
        :helperText      "Compose file source"
        :select          true
        :value           current
        :variant         "outlined"
        :margin          "normal"
        :InputLabelProps {:shrink true}
        :onChange        (fn [e]
                           (let [v (-> e .-target .-value)]
                             (if (clojure.string/starts-with? v "history-")
                               (load-history-entry! (js/parseInt (subs v 8)) history)
                               (dispatch! (routes/path-for-frontend (keyword v) {:name name})))))}
       (comp/menu-item {:key "current"  :value "stack-compose"}  "Current engine state")
       (comp/menu-item {:key "last"     :value "stack-last"
                        :disabled (not last?)} "Last deployed")
       (comp/menu-item {:key "previous" :value "stack-previous"
                        :disabled (not previous?)} "Previously deployed (rollback)")
       (when (seq history)
         (comp/menu-item {:key "history-header" :value "history-header"
                          :disabled true} "— History —"))
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
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor)
                   (state/update-value [:baseline]
                                       (get-in response [:spec :compose])
                                       state/form-state-cursor)
                   (refresh-diff!))}))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (state/update-value [:editor] editor state/form-state-cursor)
       (.on editor "change"
            (fn [cm]
              (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor)
              (refresh-diff!))))
     state)})

(defn stackfile-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-file {:name name})
    {:on-success (fn [{:keys [response]}]
                   (when (:spec response) (state/update-value [:last?] true state/form-state-cursor))
                   (when (:previousSpec response) (state/update-value [:previous?] true state/form-state-cursor))
                   (state/update-value [:history]
                                       (vec (reverse (:history response)))
                                       state/form-state-cursor))
     :on-error   (fn [_]
                   (state/update-value [:last?] false state/form-state-cursor)
                   (state/update-value [:previous?] false state/form-state-cursor)
                   (state/update-value [:history] [] state/form-state-cursor))}))

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
