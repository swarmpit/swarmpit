(ns swarmpit.component.plot
  (:require [cljsjs.plotly]
            [swarmpit.time :as time]
            [swarmpit.utils :refer [merge-data]]))

(defn default [plot-id data layout]
  (js/Plotly.react
    (.getElementById js/document plot-id)
    (clj->js data)
    (clj->js layout)
    (clj->js {:responsive             true
              :displaylogo            false
              :modeBarButtonsToRemove ["toggleSpikelines"]})))

(defn purge [plot-id]
  (js/Plotly.purge plot-id))

(defn default-layout [stats-ts options]
  (let [time (:time stats-ts)
        now (last time)
        now-1-hour (time/in-past-string 60)
        mobile? (> 600 (-> js/window .-innerWidth))]
    (merge-data
      {:autosize   true
       :showlegend (not mobile?)
       :height     300
       :margin     {:l   (if mobile? 50 70)
                    :r   (if mobile? 50 70)
                    :t   70
                    :b   70
                    :pad 0}
       :xaxis      {:range [now-1-hour now]}
       :yaxis      {:tickformat ".2f"
                    :rangemode  "tozero"}}
      options)))

(defn single [plot-id stats-ts y-key options]
  (default
    plot-id
    [{:x           (:time stats-ts)
      :y           (y-key stats-ts)
      :connectgaps false
      :fill        "tozeroy"
      :line        {:color "#52B359"}
      :type        "scatter"
      :mode        "lines"}]
    (default-layout stats-ts options)))

(defn multi [plot-id stats-ts y-key name-key options]
  (default
    plot-id
    (into []
          (map-indexed
            (fn [i item]
              (let [name (name-key item)
                    name-size (count name)]
                (merge
                  {:x           (:time item)
                   :y           (y-key item)
                   :name        (if (> name-size 15)
                                  (str (subs (name-key item) 0 15) "...") name)
                   :connectgaps false
                   :fill        "tozeroy"
                   :type        "scatter"
                   :mode        "lines"}
                  (when (zero? i)
                    {:line {:color "#52B359"}})))) stats-ts))
    (default-layout (first stats-ts) options)))