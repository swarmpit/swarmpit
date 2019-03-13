(ns swarmpit.aws.client
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]))

(def access-key-id "AKIAJZDQQHWL6PWHSGWQ")
(def secret-access-key "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")

(defn client [{:keys [service region access-key-id secret-access-key]}]
  (aws/client {:api                  service
               :region               region
               :credentials-provider (credentials/basic-credentials-provider
                                       {:access-key-id     access-key-id
                                        :secret-access-key secret-access-key})}))

(defn ecr-client [ecr-setup]
  (client {:service           :ecr
           :region            :eu-west-1
           :access-key-id     access-key-id
           :secret-access-key secret-access-key}))

(defn repositories []
  (aws/invoke
    (client {:service           :ecr
             :region            :eu-west-1
             :access-key-id     access-key-id
             :secret-access-key secret-access-key})
    {:op :DescribeRepositories}))

(defn login []
  (aws/invoke
    (client {:service           :ecr
             :region            :eu-west-1
             :access-key-id     access-key-id
             :secret-access-key secret-access-key})
    {:op :GetLogin}))

(defn user []
  (aws/invoke
    (client {:service           :iam
             :region            :eu-west-1
             :access-key-id     access-key-id
             :secret-access-key secret-access-key})
    {:op :GetUser}))


