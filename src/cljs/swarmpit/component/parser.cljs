(ns swarmpit.component.parser
  "FE component data parser"
  (:require [cljsjs.js-yaml]))

(defn parse-int
  [value]
  "Return value if integer representation otherwise nil"
  (let [parsed (js/parseInt value)]
    (when (not (js/isNaN parsed))
      parsed)))

(defn parse-float
  [value]
  "Return value if float representation otherwise nil"
  (let [parsed (js/parseFloat value)]
    (when (not (js/isNaN parsed))
      parsed)))

(defn yaml->json
  [yaml]
  "Parse YAML to JSON format"
  (js->clj (.load js/jsyaml yaml)))

(defn json->yaml
  [json]
  "Parse JSON to YAML format"
  (.dump js/jsyaml (clj->js json)))