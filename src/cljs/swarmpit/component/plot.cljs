(ns swarmpit.component.plot
  (:require [cljsjs.plotly]
            [material.components :as comp]
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

(defn- dark-mode? []
  (= "dark" (comp/current-theme-mode)))

(defn default-layout [stats-ts options]
  (let [time (:time stats-ts)
        now (last time)
        now-1-hour (time/in-past-string 60)
        mobile? (> 600 (-> js/window .-innerWidth))
        dark? (dark-mode?)
        text-color (if dark? "#ccc" "#444")
        bg-color (if dark? "#2d2d2d" "#fff")
        grid-color (if dark? "rgba(255,255,255,0.1)" "rgba(0,0,0,0.1)")]
    (merge-data
      {:autosize    true
       :showlegend  (not mobile?)
       :height      300
       :plot_bgcolor  bg-color
       :paper_bgcolor bg-color
       :font          {:color text-color}
       :margin      {:l   (if mobile? 50 70)
                     :r   (if mobile? 50 70)
                     :t   70
                     :b   70
                     :pad 0}
       :xaxis       {:range     [now-1-hour now]
                     :gridcolor grid-color
                     :linecolor grid-color
                     :tickfont  {:color text-color}}
       :yaxis       {:tickformat ".2f"
                     :rangemode  "tozero"
                     :gridcolor  grid-color
                     :linecolor  grid-color
                     :tickfont   {:color text-color}}
       :legend      {:font {:color text-color}}}
      options)))

(defn single [plot-id stats-ts y-key options]
  (default
    plot-id
    [{:x           (:time stats-ts)
      :y           (y-key stats-ts)
      :connectgaps false
      :fill        "tozeroy"
      :line        {:color (if (dark-mode?) "#1b5e20" "#52B359")}
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
                    {:line {:color (if (dark-mode?) "#1b5e20" "#52B359")}})))) stats-ts))
    (default-layout (first stats-ts) options)))