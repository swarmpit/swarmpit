(ns swarmpit.compose-diff
  (:require [clojure.string :as str]))

(defn- lcs-ops [a b]
  (let [m (count a)
        n (count b)
        dp (reduce
             (fn [dp i]
               (conj dp (reduce
                          (fn [row j]
                            (conj row (if (= (nth a (dec i)) (nth b (dec j)))
                                        (inc (get-in dp [(dec i) (dec j)]))
                                        (max (get-in dp [(dec i) j]) (peek row)))))
                          [0] (range 1 (inc n)))))
             [(vec (repeat (inc n) 0))] (range 1 (inc m)))]
    (loop [i m, j n, ops ()]
      (cond
        (and (pos? i) (pos? j) (= (nth a (dec i)) (nth b (dec j))))
        (recur (dec i) (dec j) (conj ops [:keep]))
        (and (pos? i) (or (zero? j) (>= (get-in dp [(dec i) j]) (get-in dp [i (dec j)]))))
        (recur (dec i) j (conj ops [:del (nth a (dec i))]))
        (pos? j) (recur i (dec j) (conj ops [:add]))
        :else ops))))

(defn changes [baseline current]
  (let [prefix (count (take-while true? (map = baseline current)))
        a (subvec baseline prefix)
        b (subvec current prefix)
        suffix (count (take-while true? (map = (rseq a) (rseq b))))
        ops (lcs-ops (subvec a 0 (- (count a) suffix))
                     (subvec b 0 (- (count b) suffix)))]
    (select-keys
      (reduce
        (fn [{:keys [bi chunk-start chunk-lines] :as s} [op text]]
          (case op
            :keep (-> (if (seq chunk-lines)
                        (update s :chunks conj {:b-start (or chunk-start bi)
                                               :b-end bi :lines chunk-lines})
                        s)
                      (assoc :chunk-start nil :chunk-lines [])
                      (update :bi inc))
            :add (-> s
                     (update :added conj bi)
                     (assoc :chunk-start (or chunk-start bi))
                     (update :bi inc))
            :del (-> s
                     (assoc :chunk-start (or chunk-start bi))
                     (update :chunk-lines conj text))))
        {:added #{} :chunks [] :bi prefix :chunk-start nil :chunk-lines []}
        (concat ops [[:keep]]))
      [:added :chunks])))

(defn restoration
  [current {:keys [b-start b-end lines]}]
  (let [n (count current)
        end {:line (dec n) :ch (count (peek current))}
        position #(if (< % n) {:line % :ch 0} end)]
    {:from (position b-start)
     :to   (position b-end)
     :text (str (when (= b-start n) "\n")
                (str/join "\n" lines)
                (when (< b-end n) "\n"))}))
