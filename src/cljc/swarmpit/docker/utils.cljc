(ns swarmpit.docker.utils
  "Utility ns for docker domain"
  (:refer-clojure :exclude [alias])
  (:require [clojure.string :as str]
            [swarmpit.ip :as ip]))

(defn trim-stack
  [stack name]
  "Removes stack name from object name eg. swarmpit_app -> app"
  (if (some? stack)
    (subs name (+ 1 (count stack)))
    name))

(defn in-stack?
  [stack-name map]
  "Check whenever object has same :stack property"
  (when stack-name (= stack-name (:stack map))))

(defn alias
  [key stack-name map]
  "Removes stack from name of the object belonging to that stack"
  (let [name (get map key)]
    (if (and name (in-stack? stack-name map))
      (trim-stack stack-name name)
      name)))

(defn tag
  [image]
  "Parses tag from docker image string"
  (second (str/split image #":" 2)))

(defn library?
  [repository-name]
  "Check whether repository is library"
  (let [repository-level (count (re-find #"/" repository-name))]
    (= repository-level 0)))

(defn dockerhub?
  [repository-name]
  "Check whether repository is dockerhub"
  (let [repository-level (count (re-find #"/" repository-name))]
    (and (= repository-level 1)
         (not (.contains repository-name "."))
         (not (.contains repository-name ":"))
         (not (.contains repository-name "localhost")))))

(defn registry?
  "Check whether repository is registry"
  [repository-name]
  (and (false? (library? repository-name))
       (false? (dockerhub? repository-name))))

(defn distribution-id
  [repository-name]
  "Return distribution identificator based on repository name. In case of dockerhub
   namespace is returned, if registry domain is returned"
  (first (str/split repository-name #"/")))

(defn registry-repository
  [repository-name registry-address]
  "Parse registry repository address"
  (str/replace repository-name (str registry-address "/") ""))

(defn library-repository
  [repository-name]
  "Parse library repository name"
  (str "library/" repository-name))

(defn repository
  [registry-url repository-name]
  "Return repository with registry url prefix"
  (-> (str/split registry-url #"//")
      (second)
      (str "/" repository-name)))

(defn linked-registry [image]
  "Select corresponding registry of image if linked or nil"
  (let [hypothetical-registry (first (str/split image #"/"))]
    (when (ip/is-valid-url hypothetical-registry)
      hypothetical-registry)))

(defn hypothetical-stack
  [service-name]
  "Return hypothetical stack name from canonical service name"
  (when (some? service-name)
    (let [seg (str/split service-name #"_")]
      (when (< 1 (count seg))
        (first seg)))))
