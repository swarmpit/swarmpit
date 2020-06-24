(ns swarmpit.aws.client
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [taoensso.encore :as enc]
            [taoensso.timbre :refer [error]]
            [swarmpit.log :refer [pretty-print]]))

(defn- log-error [op service result]
  (error
    (str
      "Request execution failed! Service: " (name service)
      enc/system-newline "|> Operation: " (name op)
      enc/system-newline "|< Message: " (:message result)
      enc/system-newline "|< Data: " (pretty-print result))))

(defn- client [{:keys [service region accessKeyId accessKey]}]
  (aws/client {:api                  service
               :region               (keyword region)
               :credentials-provider (credentials/basic-credentials-provider
                                       {:access-key-id     accessKeyId
                                        :secret-access-key accessKey})}))

(defn- execute [op service account]
  (let [acc (select-keys account [:region :accessKeyId :accessKey])
        result (aws/invoke
                 (client (merge acc {:service service}))
                 {:op op})]
    (if (some? (:__type result))
      (let [error (or (:message result) "Unknown error")]
        (log-error op service result)
        (throw
          (ex-info
            (str "AWS client error: " (:message result))
            {:status 401
             :type   :aws-client
             :body   {:error (str "AWS client error: " error)}})))
      result)))

(defn ecr-token [ecr]
  (-> (execute :GetAuthorizationToken :ecr ecr)
      :authorizationData
      (first)))