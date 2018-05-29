(ns swarmpit.base64
  #?(:clj
     (:import java.util.Base64)))

(def base64-regex
  #"^([0-9a-zA-Z+/]{4})*(([0-9a-zA-Z+/]{2}==)|([0-9a-zA-Z+/]{3}=))?$")

#?(:clj
   (defn encode
     [data]
     (let [credentials-bytes (.getBytes (str data))]
       (.encodeToString (Base64/getEncoder) credentials-bytes))))

#?(:clj
   (defn decode
     [encoded-data]
     (String. (.decode (Base64/getDecoder)
                       encoded-data))))

(defn base64? [data]
  (and (not= (count data) 4)
       (some? (re-matches base64-regex data))))
