(ns swarmpit.uuid
  #?(:clj
     (:import java.util.UUID)))

#?(:clj
   (defn uuid
     "Generate uuid"
     []
     (str (UUID/randomUUID))))
