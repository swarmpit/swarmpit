(ns swarmpit.test
  (:require [clojure.test :refer :all]))

(defn dind-socket-fixture
  [test]
  (swarmpit.config/update! {:docker-sock "http://localhost:12375"})
  (test))

