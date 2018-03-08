(ns swarmpit.component.parser
  "FE component data parser")

(defn parse-int
  [value]
  "Return value if integer representation otherwise nil"
  (let [parsed (js/parseInt value)]
    (when (not (js/isNaN parsed))
      parsed)))

(defn parse-float
  [value]
  "Return value if float representation otherwise nil"
  (let [parsed (js/parseFloat value)]
    (when (not (js/isNaN parsed))
      parsed)))