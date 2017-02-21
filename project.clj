(defproject swarmpit "0.1.0-SNAPSHOT"
  :description "Docker swarm management UI"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.456"]
                 [cljsjs/react "15.3.1-0"]
                 [cljsjs/react-dom "15.3.1-0"]
                 [cljsjs/material "1.3.0-0"]
                 [rum "0.10.7" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [ring "1.5.1"]
                 [compojure "1.5.2"]
                 [http-kit "2.2.0"]
                 [cheshire "5.6.3"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljc"]

  :test-paths ["test/clj" "test/cljc"]

  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/main.js"
                                    :target-path]

  :uberjar-name "swarmpit.jar"

  :main swarmpit.server

  :repl-options {:init-ns user}

  :cljsbuild {
              :builds
              [{:id           "app"
                :source-paths ["src/cljs" "src/cljc"]
                :figwheel     true
                :compiler     {:main                 swarmpit.ui
                               :foreign-libs         [{:file     "libs/material-ui/material-ui.inc.js"
                                                       :file-min "libs/material-ui/material-ui.min.inc.js"
                                                       :provides ["cljsjs.material-ui"]
                                                       :requires ["cljsjs.react" "cljsjs.react.dom"]}]
                               :externs              ["libs/material-ui/material-ui.ext.js"]
                               :asset-path           "js/out"
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "resources/public/js/out"
                               :source-map-timestamp true}}

               {:id           "min"
                :source-paths ["src/cljs" "src/cljc"]
                :jar          true
                :compiler     {:main                 swarmpit.ui
                               :foreign-libs         [{:file     "libs/material-ui/material-ui.inc.js"
                                                       :file-min "libs/material-ui/material-ui.min.inc.js"
                                                       :provides ["cljsjs.material-ui"]
                                                       :requires ["cljsjs.react" "cljsjs.react.dom"]}]
                               :externs              ["libs/material-ui/material-ui.ext.js"]
                               :output-to            "resources/public/js/main.js"
                               :output-dir           "target"
                               :source-map-timestamp true
                               :optimizations        :advanced
                               :pretty-print         false}}]}

  :figwheel {:css-dirs       ["resources/public/css"]
             :ring-handler   user/http-handler
             :server-logfile "log/figwheel.log"}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.9"]
                             [figwheel-sidecar "0.5.9"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.12"]]

              :plugins      [[lein-figwheel "0.5.9"]
                             [lein-doo "0.1.6"]]

              :source-paths ["dev"]
              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

             :uberjar
             {:source-paths ^:replace ["src/clj" "src/cljc"]
              :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
              :hooks        []
              :omit-source  true
              :aot          :all}})
