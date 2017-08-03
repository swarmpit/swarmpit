(ns swarmpit.couchdb.migration)

(defn init [] (print"init"))

(defn some-migration [] (print "some"))

(def migrations
  [init
   some-migration
   println])

(defn fname [fx]
   (->> (str fx)
        (clojure.repl/demunge)
        (re-find #"(?<=\/).*(?=@)")))

(defn namef [name]
   (-> name (symbol) (resolve)))

(defn migrate []
  (doseq [migration (->> migrations
                         (map fname)
                         (map namef))]
    (migration)))



