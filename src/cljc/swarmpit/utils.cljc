(ns swarmpit.utils
  (:require [clojure.string :as str]))

(defn remove-el
  "Remove element in `vector` on given `index`"
  [vector index]
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))

(defn select-keys* [m paths]
  "Better select-keys supporting also nested maps. E.g. [[:name] [:node :nodeName]]"
  (->> paths
       (map (fn [p]
              [(last p) (get-in m p)]))
       (into {})))

#?(:cljs
   (defn parse-int
     [value]
     "Return value if integer representation otherwise nil"
     (let [parsed (js/parseInt value)]
       (when (not (js/isNaN parsed))
         parsed))))

#?(:cljs
   (defn parse-float
     [value]
     "Return value if float representation otherwise nil"
     (let [parsed (js/parseFloat value)]
       (when (not (js/isNaN parsed))
         parsed))))

(defn trim-stack
  [stack name]
  "Removes stack name from object name eg. swarmpit_app -> app"
  (if (some? stack)
    (str/replace name #"^.*_" "")
    name))