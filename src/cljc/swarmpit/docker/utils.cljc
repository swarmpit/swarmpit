(ns swarmpit.docker.utils
  "Utility ns for docker domain"
  (:require [clojure.string :as str]))

(defn trim-stack
  [stack name]
  "Removes stack name from object name eg. swarmpit_app -> app"
  (if (some? stack)
    (subs name (+ 1 (count stack)))
    name))

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

(defn hypothetical-stack
  [service-name]
  "Return hypothetical stack name from canonical service name"
  (when (some? service-name)
    (let [seg (str/split service-name #"_")]
      (when (< 1 (count seg))
        (first seg)))))