(defproject swarmpit "1.1"
  :description "Docker swarm management UI"
  :url "http://swarmpit.io"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript "1.9.660"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/tools.logging "0.4.0"]
                 [cljsjs/react "15.4.2-2"]
                 [cljsjs/react-dom "15.4.2-2"]
                 [cljsjs/material-ui "0.18.1-0"]
                 [cljsjs/formsy-react "0.19.0-0"]
                 [cljsjs/formsy-material-ui "0.5.3-0"]
                 [rum "0.10.8" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [ring "1.5.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.0"]
                 [bk/ring-gzip "0.2.1"]
                 [buddy/buddy-auth "1.4.1"]
                 [buddy/buddy-sign "1.4.0"]
                 [buddy/buddy-hashers "1.2.0"]
                 [bidi "2.0.16"]
                 [http-kit "2.2.0"]
                 [clj-http "3.6.1"]
                 [cljs-ajax "0.5.8"]
                 [cheshire "5.6.3"]
                 [digest "1.4.5"]
                 [me.raynes/conch "0.8.0"]
                 [org.immutant/scheduling "2.1.9"]
                 [com.cemerick/url "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [environ "1.1.0"]
                 [com.github.jnr/jnr-unixsocket "0.18"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-environ "1.1.0"]
            [lein-pprint "1.1.2"]
            [lein-cloverage "1.0.9"]]
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
                               :asset-path           "js/out"
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "resources/public/js/out"
                               :parallel-build       true
                               :source-map-timestamp true}}
               {:id           "min"
                :source-paths ["src/cljs" "src/cljc"]
                :jar          true
                :compiler     {:main                 swarmpit.app
                               :externs              ["ext/material-ui.ext.js"]
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "target"
                               :source-map-timestamp true
                               :parallel-build       true
                               :closure-defines      {"goog.DEBUG" false}
                               :optimizations        :advanced
                               :pretty-print         false}}]}
  :figwheel {:css-dirs       ["resources/public/css"]
             :ring-handler   repl.user/http-handler
             :server-logfile "log/figwheel.log"}
  :profiles {:dev
             {:dependencies [[figwheel "0.5.10"]
                             [figwheel-sidecar "0.5.10"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.12"]
                             [binaryage/devtools "0.9.2"]
                             [criterium "0.4.4"]]
              :plugins      [[lein-figwheel "0.5.10"]
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