(ns swarmpit.container-integration-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [swarmpit.api :as api]
            [swarmpit.docker.engine.client :as dc]
            [swarmpit.docker.engine.http :as http]
            [swarmpit.test :refer [dind-socket-fixture]])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once dind-socket-fixture)

(defn- test-container
  [settings]
  (-> (http/execute {:method :POST :api "/containers/create"
                    :options {:headers {:Content-Type "application/json"}
                              :body (merge {:Image "hello-world:latest"} settings)}})
      :body :Id))

(defn- remove-test-container
  [id]
  (try
    (http/execute {:method :DELETE :api (str "/containers/" id)
                   :options {:query-params {:force true :v true}}})
    (catch ExceptionInfo e
      (when-not (= 404 (:status (ex-data e))) (throw e)))))

(defn- wait-for-exit
  [id]
  (http/execute {:method :POST :api (str "/containers/" id "/wait")}))

(deftest ^:integration host-container-logs-and-removal
  (doseq [tty? [false true]]
    (let [volume-name (str "container-test-" (java.util.UUID/randomUUID))
          _ (dc/create-volume {:Name volume-name})
          id (test-container {:Tty tty?
                              :HostConfig {:Mounts [{:Type "volume" :Source volume-name :Target "/data"}]}})]
      (try
        (is (= "created" (:state (api/container id))))
        (is (some #(= id (:id %)) (api/containers)))
        (api/container-action id "start")
        (wait-for-exit id)
        (is (= "exited" (:state (api/container id))))
        (is (some #(= id (:id %)) (api/containers)) "stopped containers remain visible")
        (is (str/includes? (:logs (api/container-logs id 200)) "Hello from Docker!"))
        (api/delete-container id)
        (is (not-any? #(= id (:id %)) (api/containers)))
        (is (= volume-name (:Name (dc/volume volume-name))))
        (finally
          (remove-test-container id)
          (dc/delete-volume volume-name))))))

(deftest ^:integration host-container-lifecycle
  (try (dc/image "alpine:3.20")
       (catch ExceptionInfo e
         (if (= 404 (:status (ex-data e)))
           (http/execute {:method :POST :api "/images/create" :timeout 120000
                          :options {:query-params {:fromImage "alpine" :tag "3.20"}}})
           (throw e))))
  (let [id (test-container {:Image "alpine:3.20" :Cmd ["sh" "-c" "echo stdout; echo stderr >&2; exec sleep 3600"]
                            :Labels {:com.docker.compose.project "test-project"}})]
    (try
      (is (= "test-project" (:project (api/container id))))
      (api/container-action id "start")
      (is (= "running" (:state (api/container id))))
      (is (thrown? ExceptionInfo (api/delete-container id)) "running containers cannot be removed")
      (api/container-action id "pause")
      (is (= "paused" (:state (api/container id))))
      (api/container-action id "unpause")
      (is (= "running" (:state (api/container id))))
      (api/container-action id "restart")
      (is (= "running" (:state (api/container id))))
      (api/container-action id "stop")
      (is (= "exited" (:state (api/container id))))
      (let [logs (:logs (api/container-logs id 200))]
        (is (str/includes? logs "stdout"))
        (is (str/includes? logs "stderr")))
      (api/delete-container id)
      (finally (remove-test-container id)))))

(deftest ^:integration swarm-labels-block-direct-actions
  (let [id (test-container {:Labels {:com.docker.swarm.service.id "test-service"
                                    :com.docker.swarm.task.id "test-task"}})]
    (try
      (is (true? (:managed (api/container id))))
      (is (thrown-with-msg? ExceptionInfo #"through their service" (api/container-action id "start")))
      (is (thrown-with-msg? ExceptionInfo #"through their service" (api/delete-container id)))
      (is (= "created" (:state (api/container id))))
      (finally (remove-test-container id)))))
