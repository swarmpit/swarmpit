(ns material.validation
  (:require [material.factory]
            [swarmpit.ip :as ip]
            [swarmpit.yaml :as yaml]))

(.addValidationRule js/Formsy "isValidGateway"
                    (fn [_ value]
                      (if (empty? value)
                        true
                        (ip/is-valid-gateway value))))

(.addValidationRule js/Formsy "isValidSubnet"
                    (fn [_ value]
                      (if (empty? value)
                        true
                        (ip/is-valid-subnet value))))

(.addValidationRule js/Formsy "isValidMemoryValue"
                    (fn [_ value]
                      (let [val (str value)]
                        (if (empty? val)
                          true
                          (some? (re-matches #"(([1-4]\d|[4-9])\d*)$" val))))))

(.addValidationRule js/Formsy "isValidCPUValue"
                    (fn [_ value]
                      (let [val (str value)]
                        (if (empty? val)
                          true
                          (some? (re-matches #"^(0(\.\d+)?|1(\.0+)?)$" val))))))

(.addValidationRule js/Formsy "isValidCompose"
                    (fn [_ value]
                      (if (empty? value)
                        false
                        (yaml/valid? value))))