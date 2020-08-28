(defproject swarmpit "2.0-SNAPSHOT"
  :description "Lightweight mobile-friendly Docker Swarm management UI"
  :url "http://swarmpit.io"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/core.cache "1.0.207"]
                 [org.clojure/tools.logging "1.1.0"]
                 [cljsjs/react "16.8.6-0"]
                 [cljsjs/react-dom "16.8.6-0"]
                 [cljsjs/react-window "1.8.5-0"]
                 [cljsjs/material-ui-stack "4.9.0-1"]
                 [cljsjs/codemirror "5.24.0-1"]
                 [cljsjs/js-yaml "3.3.1-0"]
                 [cljsjs/recharts "1.6.2-0"]
                 [cljsjs/plotly "1.45.3-0"]
                 [cljsjs/chartjs "2.9.3-0"]
                 [rum "0.11.2" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [sablono "0.8.4"]
                 [ring "1.8.1" :exclusions [ring/ring-jetty-adapter]]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-defaults "0.3.2" :exclusions [ring/ring-core]]
                 [bk/ring-gzip "0.3.0"]
                 [buddy/buddy-auth "1.4.1" :exclusions [cheshire]]
                 [buddy/buddy-sign "1.4.0" :exclusions [cheshire]]
                 [buddy/buddy-hashers "1.2.0"]
                 [metosin/reitit "0.3.10"]
                 [clojure-humanize "0.2.2"]
                 [http-kit "2.4.0"]
                 [enlive "1.1.6"]
                 [expound "0.7.2"]
                 [clj-time "0.15.2"]
                 [clj-http "3.10.1"]
                 [clj-commons/clj-yaml "0.7.1"]
                 [cljs-ajax "0.8.0"]
                 [cheshire "5.9.0"]
                 [digest "1.4.5"]
                 [environ "1.1.0"]
                 [jarohen/chime "0.3.2"]
                 [fullspectrum/influxdb-client "1.0.0"]
                 [me.raynes/conch "0.8.0"]
                 [com.taoensso/encore "2.120.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [com.cemerick/url "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [com.cognitect/transit-cljs "0.8.264" :exclusions [org.yaml/snakeyaml]]
                 [com.cognitect.aws/api "0.8.469"]
                 [com.cognitect.aws/endpoints "1.1.11.826"]
                 [com.cognitect.aws/ecr "801.2.708.0"]
                 [com.cognitect.aws/iam "801.2.704.0"]
                 [com.cognitect.aws/sts "798.2.678.0"]
                 [org.yaml/snakeyaml "1.18"]
                 [org.flatland/ordered "1.5.9"]
                 [com.github.jnr/jnr-unixsocket "0.18"]]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-environ "1.1.0"]
            [lein-pprint "1.1.2"]
            [lein-cloverage "1.0.9"]]
  :repositories {"local" "file:repo"}
  :min-lein-version "2.8.2"
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljc"]
  :java-source-paths ["src/java"]
  :test-selectors {:default     (complement :integration)
                   :integration :integration
                   :all         (constantly true)}
  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/main.js"
                                    :target-path]
  :uberjar-name "swarmpit.jar"
  :main swarmpit.server
  :repl-options {:init-ns repl.user}
  :cljsbuild {:builds
              [{:id           "app"
                :source-paths ["src/cljs" "src/cljc"]

                ;; The presence of a :figwheel configuration here
                ;; will cause figwheel to inject the figwheel client
                ;; into your build
                :figwheel     true
                :compiler     {:main                 swarmpit.app
                               :asset-path           "js/out"
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "resources/public/js/out"
                               :infer-externs        true
                               :parallel-build       true
                               :source-map-timestamp true
                               ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                               ;; https://github.com/binaryage/cljs-devtools
                               :preloads             [devtools.preload]}}

               ;; This next build is a compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id           "min"
                :source-paths ["src/cljs" "src/cljc"]
                :jar          true
                :compiler     {:main                 swarmpit.app
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "target"
                               :source-map-timestamp true
                               :parallel-build       true
                               :infer-externs        true
                               :optimizations        :advanced
                               :closure-defines      {"goog.DEBUG" false}
                               :pretty-print         false
                               :pseudo-names         false}}]}
  :figwheel {:css-dirs       ["resources/public/css"]
             :ring-handler   repl.user/http-handler
             :server-logfile "log/figwheel.log"}

  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:dev     {:dependencies [[figwheel "0.5.20"]
                                      [figwheel-sidecar "0.5.20"]
                                      [cider/piggieback "0.4.1"]
                                      [binaryage/devtools "0.9.10"]
                                      [criterium "0.4.4"]]
                       :plugins      [[lein-figwheel "0.5.20"]
                                      [lein-doo "0.1.6"]]
                       :source-paths ["dev"]
                       :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}
             :prod    {:source-paths ^:replace ["src/clj" "src/cljc"]
                       :prep-tasks   ["javac" "compile" ["cljsbuild" "once" "min"]]
                       :omit-source  true
                       :aot          :all}
             :uberjar [:prod]})
