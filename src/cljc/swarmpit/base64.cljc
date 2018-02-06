(ns swarmpit.base64
  #?(:clj
     (:import java.util.Base64)))

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
