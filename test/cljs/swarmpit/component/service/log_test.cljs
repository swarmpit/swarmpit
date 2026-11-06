(ns swarmpit.component.service.log-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [clojure.string :as string]
            [rum.core :as rum]
            [swarmpit.component.service.log :as log]
            [swarmpit.component.state :as state]))

(deftest timestamp-visibility-test
  (let [root (.createElement js/document "div")
        entries [{:key 1 :task "abcde12345"
                  :timestamp "2026-04-21T12:32:29.123456789Z"
                  :line "<hello>"}]]
    (try
      (doseq [show-task? [true false]]
        (testing (if show-task? "service logs" "task logs")
          (rum/mount (log/form-logs {:initialized true :timestamp false}
                                    entries entries show-task?) root)
          (is (not (string/includes? (.-textContent root) "2026-04-21")))
          (rum/mount (log/form-logs {:initialized true :timestamp true}
                                    entries entries show-task?) root)
          (is (string/includes? (.-textContent root)
                                "2026-04-21T12:32:29.123456789Z "))
          (is (= "<hello>" (.-textContent (.querySelector root ".log-body"))))
          (is (nil? (.querySelector root "hello")))
          (is (= (if show-task? 1 0) (.-length (.querySelectorAll root "a"))))
          (rum/mount (log/form-logs {:initialized true :timestamp false}
                                    entries entries show-task?) root)
          (is (not (string/includes? (.-textContent root) "2026-04-21")))))
      (testing "missing timestamps and blank lines"
        (let [entries [{:key 2 :task "abcde12345" :line ""}]]
          (rum/mount (log/form-logs {:initialized true :timestamp true}
                                    entries entries false) root)
          (is (= " " (.-textContent root)))))
      (finally
        (rum/unmount root)))))

(deftest timestamp-toggle-test
  (let [root (.createElement js/document "div")
        original @state/state
        initial {:timestamp false :filter {:predicate "needle"} :history "60m"}]
    (try
      (.appendChild js/document.body root)
      (state/set-value initial log/form-state-cursor)
      (doseq [enabled? [false true]]
        (rum/mount (log/form-timestamp enabled?) root)
        (let [button (.querySelector root "button")]
          (is (= "Show timestamps" (.getAttribute button "aria-label")))
          (is (= (str enabled?) (.getAttribute button "aria-pressed")))
          (.click button)
          (is (= (assoc initial :timestamp (not enabled?))
                 (state/get-value log/form-state-cursor)))))
      (finally
        (rum/unmount root)
        (.removeChild js/document.body root)
        (reset! state/state original)))))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [summary]
  (set! js/window.cljsTestResult (clj->js summary)))

(enable-console-print!)
(run-tests)
