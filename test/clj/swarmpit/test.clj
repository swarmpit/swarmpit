(ns swarmpit.test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [swarmpit.database :as db]
            [swarmpit.setup :as setup]
            [swarmpit.config :as config]
            [swarmpit.docker.engine.client :as docker]))

(defn dind-socket-fixture
  [test]
  (config/update! {:docker-sock "http://localhost:12375"})
  (setup/docker)
  (test)
  (config/update! {}))

(defn running-service-fixture
  [test]
  (let [id (-> (docker/create-service
                 nil
                 (-> (slurp "test/clj/swarmpit/docker/engine/service.edn")
                     (edn/read-string)
                     (merge {:Name "test-service"}))) :ID)]
    (test)
    (docker/delete-service id)))

(defn db-init-fixture
  [test]
  (db/init)
  (test))