(ns swarmpit.ip)

(def valid-ip-regex
  #"\b(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\b")

(def valid-cidr-regex
  #"\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/(3[0-2]|[1-2]?[0-9])\b")

(def valid-url
  #"(http(s)?://)?([\w-]+\.)+[\w-]+(/[\w- ;,./?%&=]*)?")

(defn is-valid-gateway
  [ip]
  (some? (re-matches valid-ip-regex ip)))

(defn is-valid-subnet
  [ip-cidr]
  (some? (re-matches valid-cidr-regex ip-cidr)))

(defn is-valid-url
  [url]
  (some? (re-matches valid-url url)))
