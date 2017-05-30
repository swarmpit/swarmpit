(ns swarmpit.agent
  (:require [clojure.core.memoize :as memo]
            [clojure.core.async :as async :refer [<! <!! go-loop]]
            [chime :refer [chime-ch]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [swarmpit.api :as api]))

(defn start-repository-agent
  []
  (go-loop [repositories (api/repositories)]
    (<! (async/timeout 10000))
    (println "Fetching repository data...")
    (memo/memo-swap! api/cached-repositories {:data repositories})))

;(defn start-repository-agent
;  []
;  (let [chimes (chime-ch [(-> t/now)
;                          (-> 10 t/seconds t/from-now)])]
;    (<!! (go-loop []
;           (when-let [msg (<! chimes)]
;             (prn "Chiming at:" msg)
;             (recur))))))