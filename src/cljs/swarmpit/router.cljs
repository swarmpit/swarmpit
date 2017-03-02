(ns swarmpit.router)

(defn dispatch!
  [url]
  (-> js/document
      .-location
      (set! url)))
