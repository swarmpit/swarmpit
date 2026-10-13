(ns swarmpit.container-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [swarmpit.api :as api]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.docker.engine.client :as dc]
            [swarmpit.docker.engine.http :as http]
            [swarmpit.docker.engine.log :as log]
            [swarmpit.docker.engine.mapper.inbound :as mapper]
            [swarmpit.server :as server]
            [swarmpit.token :as token])
  (:import (java.nio ByteBuffer)
           (java.nio.charset StandardCharsets)
           (clojure.lang ExceptionInfo)))

(def ^:private id (apply str (repeat 64 "a")))

(def ^:private container
  {:Id id :Name "/web" :Created "2026-01-01T00:00:00Z"
   :Path "/bin/sh" :Args ["-c" "sleep 3600"]
   :Config {:Image "alpine:3.20" :Tty false :Labels nil :Env ["PASSWORD=example"]}
   :State {:Status "exited" :ExitCode 17 :Error ""}
   :HostConfig {:RestartPolicy {:Name "no"}}
   :NetworkSettings {:Ports {:80/tcp [{:HostIp "0.0.0.0" :HostPort "8080"}]
                            :90/udp nil}
                     :Networks {:bridge {:IPAddress "172.17.0.2"}}}
   :Mounts [{:Type "bind" :Source "/tmp/data" :Destination "/data" :RW false}]})

(defn- bytes-utf8 [s]
  (.getBytes ^String s StandardCharsets/UTF_8))

(defn- frame [stream data]
  (let [data (if (string? data) (bytes-utf8 data) data)]
    (.array (doto (ByteBuffer/allocate (+ 8 (alength ^bytes data)))
              (.put (byte stream))
              (.put (byte-array 3))
              (.putInt (alength ^bytes data))
              (.put ^bytes data)))))

(deftest container-mapping
  (testing "stopped, standalone and Compose containers retain their identity"
    (with-redefs [dc/info (constantly {:Name "manager" :Swarm {:NodeID "local-node"}})
                  dc/containers (constantly [{:Id id :Names ["/web"] :Image "alpine:3.20"
                                            :State "exited" :Status "Exited (17)" :Labels nil}
                                           {:Id "second" :Names ["/project-web-1"] :Labels {:com.docker.compose.project "project"}}])]
      (let [[standalone compose] (api/containers)]
        (is (= {:id id :containerName "web" :image "alpine:3.20" :state "exited"
                :status "Exited (17)" :managed false :project nil :serviceId nil :serviceName nil
                :taskId nil :nodeId "local-node" :nodeName "manager"}
               standalone))
        (is (= "project" (:project compose)))
        (is (false? (:managed compose))))))
  (testing "inspect includes ports, mounts, networks and exit details without returning raw configuration"
    (let [mapped (mapper/->container container)]
      (is (= "web" (:containerName mapped)))
      (is (= 17 (:exitCode mapped)))
      (is (= ["/bin/sh" "-c" "sleep 3600"] (:command mapped)))
      (is (= [{:source "/tmp/data" :target "/data" :type "bind" :readOnly true :volumeName nil}] (:mounts mapped)))
      (is (= [{:id nil :networkName "bridge" :address "172.17.0.2"}] (:networks mapped)))
      (is (= [{:containerPort "80/tcp" :hostIp "0.0.0.0" :hostPort "8080"}
              {:containerPort "90/udp"}] (:ports mapped)))
      (is (not (contains? mapped :Config)))
      (is (not (contains? mapped :Env)))))
  (testing "absent names, networks and mounts are tolerated"
    (is (= id (:containerName (mapper/->container-summary {:Id id}))))
    (is (= [] (:mounts (mapper/->container (dissoc container :Mounts)))))
    (is (= [] (:networks (mapper/->container (assoc container :NetworkSettings nil)))))))

(deftest container-resource-links
  (let [mapped (mapper/->container
                 (-> container
                     (assoc-in [:Config :Labels] {:com.docker.swarm.service.id "service"
                                                  :com.docker.swarm.service.name "web"
                                                  :com.docker.swarm.task.id "task"})
                     (assoc-in [:NetworkSettings :Networks :bridge :NetworkID] "network")
                     (assoc :Mounts [{:Type "volume" :Name "data" :Source "/var/lib/docker/volumes/data/_data"
                                      :Destination "/data" :RW true}])))]
    (is (= "service" (:serviceId mapped)))
    (is (= "task" (:taskId mapped)))
    (is (= "network" (get-in mapped [:networks 0 :id])))
    (is (= "data" (get-in mapped [:mounts 0 :volumeName])))))

(deftest connected-node-container-scope
  (with-redefs [dc/info (constantly {:Name "manager" :Swarm {:NodeID "local-node"}})
                dc/node (fn [id] {:ID (if (= "manager" id) "local-node" id)})
                api/node-stats identity]
    (is (true? (:containersAvailable (api/node "local-node"))))
    (is (true? (:containersAvailable (api/node "manager"))))
    (is (false? (:containersAvailable (api/node "worker"))))))

(deftest swarm-container-lifecycle-is-rejected
  (doseq [labels [{:com.docker.swarm.service.id "service"}
                  {:com.docker.swarm.task.id "task"}]
          action ["start" "stop" "restart" "pause" "unpause" "delete"]]
    (with-redefs [dc/container (constantly (assoc-in container [:Config :Labels] labels))
                  http/execute (fn [_] (is false "must not mutate a Swarm container"))]
      (let [exception (try
                        (if (= "delete" action)
                          (api/delete-container id)
                          (api/container-action id action))
                        (catch ExceptionInfo e e))]
        (is (= 409 (:status (ex-data exception))))))))

(deftest standalone-actions-resolve-to-the-inspected-id
  (let [requests (atom [])]
    (with-redefs [dc/container (constantly (assoc-in container [:Config :Labels] {:com.docker.compose.project "project"}))
                  http/execute (fn [request] (swap! requests conj request) {:status 204})]
      (doseq [action ["start" "stop" "restart" "pause" "unpause"]]
        (api/container-action (subs id 0 12) action))
      (api/delete-container (subs id 0 12)))
    (is (= (mapv #(str "/containers/" id "/" %) ["start" "stop" "restart" "pause" "unpause"])
           (mapv :api (butlast @requests))))
    (doseq [request (take 2 (rest @requests))]
      (is (= 10 (get-in request [:options :query-params :t])))
      (is (= 20000 (:timeout request))))
    (is (= {:method :DELETE :api (str "/containers/" id)
            :options {:query-params {:force false :v false}}}
           (last @requests))))
  (with-redefs [dc/container (fn [_] (is false "invalid action must be rejected before Docker is called"))]
    (is (thrown-with-msg? ExceptionInfo #"Unsupported" (api/container-action id "kill")))))

(deftest docker-container-list-and-logs-options
  (let [requests (atom [])]
    (with-redefs [http/execute (fn [request] (swap! requests conj request) {:body []})]
      (dc/containers)
      (dc/container-logs id 200))
    (is (= {:all true} (get-in @requests [0 :options :query-params])))
    (is (= :byte-array (get-in @requests [1 :options :as])))
    (is (= {:stdout true :stderr true :timestamps true :follow false :tail 200}
           (get-in @requests [1 :options :query-params])))))

(deftest docker-log-decoding
  (testing "frame lengths count bytes, including long lines, UTF-8 and embedded newlines"
    (let [stdout (str "2026-01-01T00:00:00Z " (apply str (repeat 300 "ž")) "\nnext line\n")
          stderr "2026-01-01T00:00:01Z error\n"]
      (is (= (str stdout stderr)
             (log/parse-container-log (byte-array (concat (frame 1 stdout) (frame 2 stderr))) false)))))
  (testing "UTF-8 bytes may span frames"
    (let [data (bytes-utf8 "žluťoučký")]
      (is (= "žluťoučký" (log/parse-container-log
                           (byte-array (concat (frame 1 (byte-array (take 1 data)))
                                               (frame 1 (byte-array (drop 1 data))))) false)))))
  (testing "TTY output is plain text and may itself be JSON"
    (doseq [output ["hello\r\nžluťoučký\r\n" "{\"message\":\"hello\"}" ""]]
      (is (= output (log/parse-container-log (bytes-utf8 output) true)))))
  (is (= "" (log/parse-container-log (byte-array 0) false)))
  (testing "truncated or invalid frames fail without returning framing bytes"
    (doseq [data [(byte-array [1 0 0])
                  (byte-array [1 0 0 0 0 0 0 5 65])
                  (byte-array [1 0 0 0 -1 -1 -1 -1])
                  (frame 4 "oops")]]
      (is (thrown-with-msg? ExceptionInfo #"Invalid Docker log stream" (log/parse-container-log data false)))))
  (testing "the container TTY setting selects the decoder"
    (doseq [tty? [true false]]
      (with-redefs [dc/container (constantly (assoc-in container [:Config :Tty] tty?))
                    dc/container-logs (fn [resolved-id tail]
                                        (is (= id resolved-id))
                                        (is (= 200 tail))
                                        (if tty? (bytes-utf8 "hello\n") (frame 1 "hello\n")))]
        (is (= {:logs "hello\n"} (api/container-logs id 200)))))))

(defn- request
  [role method path & [query]]
  (with-redefs [cc/secret (constantly "container-test-secret")
                cc/user-by-username (fn [username] {:username username :role role})]
    (server/app {:request-method method :uri path :query-string query
                 :headers (cond-> {"accept" "application/json"}
                            role (assoc "authorization" (token/generate-jwt {:username "tester" :role role})))})))

(deftest container-routes-enforce-roles-and-inputs
  (with-redefs [dc/info (constantly {:Name "manager" :Swarm {:NodeID "local-node"}})
                dc/containers (constantly [])
                dc/container (constantly container)
                dc/container-logs (fn [_ _] (frame 1 "hello\n"))
                dc/container-action (fn [_ _])
                dc/delete-container (fn [_])]
    (doseq [path ["/api/containers" (str "/api/containers/" id) (str "/api/containers/" id "/logs")]]
      (is (= 401 (:status (request nil :get path))))
      (doseq [role ["guest" "user" "admin"]]
        (is (= 200 (:status (request role :get path))))))
    (doseq [[method suffix] [[:post "/start"] [:post "/stop"] [:post "/restart"]
                             [:post "/pause"] [:post "/unpause"] [:delete ""]]]
      (let [path (str "/api/containers/" id suffix)]
        (is (= 401 (:status (request nil method path))))
        (is (= 403 (:status (request "guest" method path))))
        (doseq [role ["user" "admin"]]
          (is (= 200 (:status (request role method path)))))))
    (doseq [query ["tail=0" "tail=-1" "tail=5001" "tail=all" "tail=1.5"]]
      (is (= 400 (:status (request "user" :get (str "/api/containers/" id "/logs") query)))))
    (with-redefs [dc/container-logs (fn [_ tail] (is (= 25 tail)) (frame 1 "hello\n"))]
      (is (= 200 (:status (request "guest" :get (str "/api/containers/" id "/logs") "tail=25")))))
    (is (= 400 (:status (request "user" :post (str "/api/containers/" id "/kill")))))
    (is (= 400 (:status (request "user" :get "/api/containers/not-an-id"))))
    (with-redefs [dc/container (constantly (assoc-in container [:Config :Labels] {:com.docker.swarm.service.id "service"}))]
      (is (= 409 (:status (request "user" :post (str "/api/containers/" id "/stop")))))
      (is (= 409 (:status (request "admin" :delete (str "/api/containers/" id))))))))

(deftest container-swagger-is-published
  (let [response (request nil :get "/api/swagger.json")
        paths (get (json/parse-string (slurp (io/reader (:body response)))) "paths")]
    (is (contains? (get paths "/api/containers") "get"))
    (is (= #{"get" "delete"} (set (keys (get paths "/api/containers/{id}")))))
    (is (contains? (get paths "/api/containers/{id}/logs") "get"))
    (is (contains? (get paths "/api/containers/{id}/{action}") "post"))))
