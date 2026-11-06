(ns swarmpit.couchdb-client-test
  (:require [clojure.test :refer :all]
            [swarmpit.config :as config]
            [swarmpit.couchdb.client :as db]
            [swarmpit.docker.secret :as secret]
            [swarmpit.http :as http]))

(defn- request-with [settings]
  (let [request (atom nil)]
    (with-redefs [config/config settings
                  http/execute-in-scope (fn [r] (reset! request r) {:body {}})]
      (db/version)
      @request)))

(deftest couchdb-authentication
  (testing "legacy unauthenticated connections keep their existing behavior"
    (let [request (request-with {:db-url "http://db:5984"})]
      (is (= "http://db:5984/" (:url request)))
      (is (nil? (get-in request [:options :basic-auth])))))
  (testing "special characters are passed as credentials, never interpolated into the URL"
    (let [request (request-with {:db-url "http://db:5984"
                                :db-username "swarmpit"
                                :db-password "a:b@c/$#%+"})]
      (is (= "http://db:5984/" (:url request)))
      (is (= ["swarmpit" "a:b@c/$#%+"] (get-in request [:options :basic-auth])))))
  (testing "a Docker secret takes precedence over the password environment variable"
    (with-redefs [secret/get (fn [name] (when (= "db_password" name) "secret-value"))]
      (is (= ["swarmpit" "secret-value"]
             (get-in (request-with {:db-url "http://db:5984" :db-username "swarmpit"
                                    :db-password "ignored" :db-password-secret "db_password"})
                     [:options :basic-auth]))))))

(deftest couchdb-misconfiguration-fails-closed
  (doseq [settings [{:db-username "swarmpit"}
                    {:db-password "password-without-username"}
                    {:db-username "swarmpit" :db-password ""}
                    {:db-username "swarmpit" :db-password "fallback" :db-password-secret "missing"}]]
    (with-redefs [secret/get (constantly nil)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"CouchDB credentials"
            (request-with (assoc settings :db-url "http://db:5984")))))))
