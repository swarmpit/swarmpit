(ns swarmpit.base64
  #?(:clj  (:import java.util.Base64)
     :cljs (:require [goog.crypt.base64 :as b64])))

(def base64-regex
  #"^(?:[A-Za-z0-9+\/]{4})*(?:[A-Za-z0-9+\/]{2}==|[A-Za-z0-9+\/]{3}=)?$")

#?(:clj
   (defn encode
     [data]
     (let [credentials-bytes (.getBytes (str data))]
       (.encodeToString (Base64/getEncoder) credentials-bytes))))

#?(:clj
   (defn decode
     [encoded-data]
     (String. (.decode (Base64/getDecoder)
                       encoded-data)))
   :cljs
   (defn decode
     [encoded-data]
     (b64/decodeString encoded-data)))


(defn base64? [data]
  (and (= 0 (mod (count data) 4))
       (some? (re-matches base64-regex data))
       (let [str (decode data)]
         (->> (map #?(:clj int :cljs #(.charCodeAt % 0)) str)
              (reduce +)
              (> (* 128 (count str)))))))
