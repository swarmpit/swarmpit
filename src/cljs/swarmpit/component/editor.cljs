(ns swarmpit.component.editor
  (:require [cljsjs.codemirror]
            [cljsjs.codemirror.addon.lint.lint]
            [cljsjs.codemirror.addon.lint.yaml-lint]
            [cljsjs.codemirror.mode.yaml]))

(defn factory
  [editor-mode editor-id]
  (js/CodeMirror.fromTextArea
    (.getElementById js/document editor-id)
    (clj->js
      {:lineNumbers       true
       :viewportMargin    (.-Infinity js/window)
       :matchBrackets     true
       :smartIndent       true
       :tabSize           2
       :indentWithTabs    false
       :autofocus         true
       :autoCloseBrackets true
       :gutters           ["CodeMirror-lint-markers"]
       :lint              true
       :mode              editor-mode})))

(defn yaml
  [editor-id]
  (factory "yaml" editor-id))