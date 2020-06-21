(ns swarmpit.yaml
  (:require
    #?@(:clj  [[clj-yaml.core :as yaml]
               [flatland.ordered.map :refer [ordered-map]]]
        :cljs [[cljsjs.js-yaml]]))
  #?(:clj
     (:import [org.yaml.snakeyaml Yaml DumperOptions]
              (clojure.lang IPersistentMap))))

#?(:clj
   (extend-protocol yaml/YAMLCodec
     IPersistentMap
     (encode [data]
       (into (ordered-map)
             (for [[k v] data]
               [(yaml/encode k) (yaml/encode v)])))))

#?(:clj
   (defn- generate-string
     [data]
     (.dump (Yaml. (doto (DumperOptions.)
                     (.setDefaultFlowStyle (:block yaml/flow-styles))
                     (.setIndicatorIndent 1)))
            (yaml/encode data))))

(defn ->yaml
  [map]
  "Parse YAML to JSON format"
  #?(:clj  (generate-string map)
     :cljs (.dump js/jsyaml (clj->js map))))

(defn ->json
  [yaml]
  "Parse YAML to JSON format"
  #?(:clj  (yaml/parse-string yaml)
     :cljs (js->clj (.load js/jsyaml yaml))))

#?(:cljs
   (defn valid?
     [yaml]
     "Check if valid yaml"
     (try
       (some? (->json yaml))
       (catch :default _ false))))
