(ns swarmpit.api-integration-test
  (:require [swarmpit.test :refer :all]
            [swarmpit.api :refer :all]
            [swarmpit.docker.engine.client :as client]
            [clojure.test :refer :all]
            [clojure.edn :as edn]))

(use-fixtures :once db-init-fixture dind-socket-fixture running-service-fixture)

(defn test-crud
  [crud]
  (let [{:keys [name spec create read list update delete]} crud]
    (let [id (:id (create spec))
          created (read id)]
      (is (some? id))
      (is (some? created))
      (when name
        (is (some? (read name))))
      (is (not (empty? (list))))
      (when update
        (update id created)
        (is (< (:version created) (:version (read id)))))
      (delete id)
      (is (thrown? Exception (read id))))))

(deftest ^:integration docker
  (let [service-id (-> (client/services) first :ID)]

    (testing "services"
      (is (some? (services))))

    (testing "service"
      (is (some? (service service-id)))
      (is (= service-id (:id (service service-id))))
      (is (= (-> (services) first) (service service-id)))
      (is (some? (service-networks service-id)))
      (is (some? (service-tasks service-id))))

    (testing "scale service"
      (let [id (->> (edn/read-string (slurp "test/clj/swarmpit/create-service.edn"))
                    (create-service nil)
                    :id)
            created (service id)]
        (is (= "hello-world" (-> created :repository :name)))
        (is (= 1 (-> created :replicas)))
        (update-service nil id (merge created {:replicas 2}))
        (is (= 2 (-> id (service) :replicas)))
        (delete-service id)))

    (testing "crud services"
      (test-crud {:name   "created"
                  :spec   (-> "test/clj/swarmpit/create-service.edn"
                              (slurp)
                              (edn/read-string))
                  :create (fn [spec] (create-service nil spec))
                  :read   service
                  :list   services
                  :update (fn [id spec] (update-service nil id spec))
                  :delete delete-service}))

    (testing "secrets"
      (is (some? (secrets))))

    (testing "crud secrets"
      (test-crud {:name   nil ; docker 1.13 doesn't support query by name
                  :spec   {:secretName "test-secret"
                           :encode     true
                           :data       "asdf"}
                  :create create-secret
                  :read   secret
                  :list   secrets
                  :update update-secret
                  :delete delete-secret}))

    (testing "networks"
      (let [networks (networks)
            some-network (-> networks first)]
        (is (some? networks))
        (is (some? (:id some-network)))
        ; TODO: following assert should be fixed, there are no reason that date in list should be different
        (is (not (= (:created some-network) (:created (network (:id some-network))))))
        (is (= "overlay" (:driver some-network)))
        (is (= "swarm" (:scope some-network)))))

    (testing "crud networks"
      (test-crud {:name   "test-net"
                  :spec   {:networkName "test-net"
                           :internal    false
                           :driver      "overlay"}
                  :create create-network
                  :read   network
                  :list   networks
                  :delete delete-network}))

    (testing "volumes"
      (is (some? (volumes))))

    (testing "crud volumes"
      (test-crud {:name   "test-volume"
                  :spec   {:volumeName "test-volume"
                           :driver     "local"}
                  :create create-volume
                  :read   volume
                  :list   volumes
                  :delete delete-volume}))

    (testing "nodes"
      (let [nodes (nodes)
            some-node (-> nodes first)]
        (is (some? nodes))
        (is (some? (:id some-node)))
        (is (= some-node (node (:id some-node))))
        (is (= "manager" (:role some-node)))
        (is (= "ready" (:state some-node)))))

    (testing "tasks"
      (let [tasks (tasks)]
        (is (some? tasks))))

    (testing "find public repository"
      (let [results (public-repositories "nginx" 1)]
        (is (some? results))
        (is (= "nginx" (:query results)))
        (is (= "nginx" (-> results :results first :name)))))))
