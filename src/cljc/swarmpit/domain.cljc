(ns swarmpit.domain)

(defrecord Service [id name image mode ports])

(defrecord Port [protocol targetPort publishedPort])
