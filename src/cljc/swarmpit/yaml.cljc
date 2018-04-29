(ns swarmpit.yaml
  (:require
    #?@(:clj  [[clj-yaml.core :as yaml]]
        :cljs [[cljsjs.js-yaml]])))

#?(:clj
   (defn ->json
     [yaml]
     "Parse YAML to JSON format"
     (yaml/parse-string yaml)))


#?(:clj
   (defn ->yaml
     [map]
     "Parse YAML to JSON format"
     (yaml/generate-string map :dumper-options {:flow-style :block})))

#?(:cljs
   (defn ->json
     [yaml]
     "Parse YAML to JSON format"
     (js->clj (.load js/jsyaml yaml))))

#?(:cljs
   (defn ->yaml
     [json]
     "Parse JSON to YAML format"
     (.dump js/jsyaml (clj->js json))))

#?(:cljs
   (defn valid?
     [yaml]
     "Check if valid yaml"
     (try
       (some? (->json yaml))
       (catch :default _ false))))
