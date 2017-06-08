(ns swarmpit.uuid
  #?(:clj
     (:import java.util.UUID)))

#?(:clj
   (defn uuid
     "Generate cuuid"
     []
     (str (UUID/randomUUID))))
