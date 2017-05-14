(ns swarmpit.uri)

(defn dispatch!
  [url]
  (-> js/document
      .-location
      (set! url)))