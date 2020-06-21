(ns material.factory
  (:require [sablono.util :refer [camel-case-keys]]))

(set! *warn-on-infer* true)

(def props-kebab->camel->js (comp clj->js camel-case-keys))

(defn create-mui-cmp
  ([react-class args]
   (let [first-arg (first args)
         args (if (or (map? first-arg)
                      (nil? first-arg))
                args
                (cons {} args))]
     (apply js/React.createElement
            react-class
            (props-kebab->camel->js (first args))
            (rest args))))
  ([root-obj type args]
   (create-mui-cmp (aget root-obj type) args)))

(defn create-element [comp opts & children]
  (apply js/React.createElement comp (clj->js opts) children))

(defn create-js-element [comp opts & children]
  (apply js/React.createElement comp opts children))