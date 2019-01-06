(defproject swarmpit "1.6-SNAPSHOT"
  :description "Docker swarm management UI"
  :url "http://swarmpit.io"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/core.cache "0.7.1"]
                 [org.clojure/tools.logging "0.4.1"]
                 [cljsjs/react "16.4.0-0"]
                 [cljsjs/react-dom "16.4.0-0"]
                 [cljsjs/material-ui "3.2.0-0"]
                 [cljsjs/material-ui-icons "3.0.1-0"]
                 [cljsjs/react-select "2.1.1"]
                 [cljsjs/emotion "9.2.12"]
                 [cljsjs/react-input-autosize "2.2.1-1"]
                 [cljsjs/react-autosuggest "9.3.4-0"]
                 [cljsjs/rc-slider "8.6.1-0"]
                 [cljsjs/codemirror "5.24.0-1"]
                 [cljsjs/js-yaml "3.3.1-0"]
                 [cljsjs/recharts "1.1.0-3"]
                 [rum "0.11.2" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [sablono "0.8.4"]
                 [ring "1.6.3" :exclusions [ring/ring-jetty-adapter]]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.1" :exclusions [ring/ring-core]]
                 [bk/ring-gzip "0.3.0"]
                 [buddy/buddy-auth "1.4.1" :exclusions [cheshire]]
                 [buddy/buddy-sign "1.4.0" :exclusions [cheshire]]
                 [buddy/buddy-hashers "1.2.0"]
                 [clojure-humanize "0.2.2"]
                 [http-kit "2.2.0"]
                 [clj-http "3.8.0"]
                 [clj-yaml "0.4.0"]
                 [cljs-ajax "0.5.8"]
                 [bidi "2.0.16"]
                 [kibu/pushy "0.3.8"]
                 [cheshire "5.6.3"]
                 [digest "1.4.5"]
                 [environ "1.1.0"]
                 [me.raynes/conch "0.8.0"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [com.cemerick/url "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [com.cognitect/transit-cljs "0.8.256" :exclusions [org.yaml/snakeyaml]]
                 [org.yaml/snakeyaml "1.18"]
                 [org.flatland/ordered "1.5.6"]
                 [com.github.jnr/jnr-unixsocket "0.18"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]
            [lein-pprint "1.1.2"]
            [lein-cloverage "1.0.9"]]
  :repositories {"local" "file:repo"}
  :min-lein-version "2.6.1"
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
                :figwheel     true
                :compiler     {:main                 swarmpit.app
                               :preloads             [devtools.preload]
                               :asset-path           "/js/out"
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "resources/public/js/out"
                               :infer-externs        true
                               :parallel-build       true
                               :source-map-timestamp true}}
               {:id           "min"
                :source-paths ["src/cljs" "src/cljc"]
                :jar          true
                :compiler     {:main                 swarmpit.app
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "target"
                               :source-map-timestamp true
                               :parallel-build       true
                               :closure-defines      {"goog.DEBUG" false}
                               :optimizations        :advanced
                               :infer-externs        true
                               :pretty-print         false}}]}
  :figwheel {:css-dirs       ["resources/public/css"]
             :ring-handler   repl.user/http-handler
             :server-logfile "log/figwheel.log"}
  :profiles {:dev
             {:dependencies [[figwheel "0.5.17"]
                             [figwheel-sidecar "0.5.17"]
                             [com.cemerick/piggieback "0.2.2"]
                             [org.clojure/tools.nrepl "0.2.13"]
                             [binaryage/devtools "0.9.10"]
                             [criterium "0.4.4"]]
              :plugins      [[lein-figwheel "0.5.17"]
                             [lein-doo "0.1.6"]]
              :source-paths ["dev"]
              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :prod
             {:source-paths ^:replace ["src/clj" "src/cljc"]
              :prep-tasks   ["javac" "compile" ["cljsbuild" "once" "min"]]
              :omit-source  true
              :aot          :all}
             :uberjar
             [:prod]})
