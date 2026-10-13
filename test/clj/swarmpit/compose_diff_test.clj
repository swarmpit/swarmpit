(ns swarmpit.compose-diff-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [swarmpit.compose-diff :as diff]))

(defn- restore
  [current change]
  (let [lines (vec (str/split current #"\n" -1))
        {:keys [from to text]} (diff/restoration lines change)
        offset (fn [{:keys [line ch]}]
                 (+ ch (reduce + (map #(inc (count %)) (take line lines)))))]
    (str (subs current 0 (offset from)) text (subs current (offset to)))))

(deftest restore-deleted-lines
  (doseq [[label current change expected]
          [["first line" "services:\n  web: {}\n"
            {:b-start 0 :b-end 0 :lines ["version: '3'"]}
            "version: '3'\nservices:\n  web: {}\n"]
           ["first block" "services:\n  web: {}"
            {:b-start 0 :b-end 0 :lines ["# application" "version: '3'"]}
            "# application\nversion: '3'\nservices:\n  web: {}"]
           ["middle line" "services:\n    image: nginx\n"
            {:b-start 1 :b-end 1 :lines ["  web:"]}
            "services:\n  web:\n    image: nginx\n"]
           ["last line without newline" "services:\n  web:"
            {:b-start 2 :b-end 2 :lines ["    image: nginx"]}
            "services:\n  web:\n    image: nginx"]
           ["last line with newline" "services:\n  web:\n"
            {:b-start 2 :b-end 2 :lines ["    image: nginx"]}
            "services:\n  web:\n    image: nginx\n"]
           ["trailing newline" "services: {}"
            {:b-start 1 :b-end 1 :lines [""]}
            "services: {}\n"]
           ["empty editor" ""
            {:b-start 0 :b-end 1 :lines ["services: {}"]}
            "services: {}"]
           ["empty editor with newline" ""
            {:b-start 0 :b-end 0 :lines ["services: {}"]}
            "services: {}\n"]]]
    (testing label
      (is (= expected (restore current change))))))

(deftest restore-replaced-lines
  (doseq [[label current change expected]
          [["first line" "version: '2'\nservices: {}"
            {:b-start 0 :b-end 1 :lines ["version: '3'"]}
            "version: '3'\nservices: {}"]
           ["middle block" "services:\n  old: {}\nnetworks: {}\n"
            {:b-start 1 :b-end 2 :lines ["  web:" "    image: nginx"]}
            "services:\n  web:\n    image: nginx\nnetworks: {}\n"]
           ["last line without newline" "services:\n  web:\n    image: busybox"
            {:b-start 2 :b-end 3 :lines ["    image: nginx"]}
            "services:\n  web:\n    image: nginx"]
           ["last line with newline" "services:\n  web:\n    image: busybox\n"
            {:b-start 2 :b-end 3 :lines ["    image: nginx"]}
            "services:\n  web:\n    image: nginx\n"]
           ["whole file" "old: {}"
            {:b-start 0 :b-end 1 :lines ["services:" "  web: {}"]}
            "services:\n  web: {}"]]]
    (testing label
      (is (= expected (restore current change))))))
