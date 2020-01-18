(ns swarmpit.base64
  #?(:clj  (:import java.util.Base64)
     :cljs (:require [goog.crypt :as crypt]
                     [goog.crypt.base64 :as b64])))

#?(:clj
   (defn encode
     [data]
     (let [credentials-bytes (.getBytes (str data))]
       (.encodeToString (Base64/getEncoder) credentials-bytes)))
   :cljs
   (defn encode
     [data]
     (-> (crypt/stringToUtf8ByteArray data)
         (b64/encodeByteArray))))

#?(:clj
   (defn decode
     [encoded-data]
     (String. (.decode (Base64/getDecoder)
                       encoded-data)))
   :cljs
   (defn decode
     [encoded-data]
     (-> (b64/decodeStringToByteArray encoded-data)
         (crypt/utf8ByteArrayToString))))
