(defproject swarmpit "0.1.0-SNAPSHOT"
  :description "Docker swarm management UI"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.456"]
                 [figwheel-sidecar "0.5.9"]
                 [cljsjs/react "15.3.1-0"]
                 [cljsjs/react-dom "15.3.1-0"]
                 [cljsjs/material "1.3.0-0"]
                 [rum "0.10.7" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [rum-mdl "0.2.0"]
                 [secretary "1.2.3"]
                 [cheshire "5.6.3"]
                 [http-kit "2.1.19"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.9"]]
  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/main.js"
                                    :target-path]
  :source-paths ["src/clj", "script", "libs"]
  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/cljs"]
                        :figwheel     true
                        :compiler     {:main         "swarmpit.app"
                                       :asset-path   "js/out"
                                       :foreign-libs [{:file     "libs/material-ui/material-ui.inc.js"
                                                       :file-min "libs/material-ui/material-ui.min.inc.js"
                                                       :provides ["cljsjs.material-ui"]
                                                       :requires ["cljsjs.react" "cljsjs.react.dom"]}]
                                       :externs      ["libs/material-ui/material-ui.ext.js"]
                                       :output-to    "resources/public/js/main.js"
                                       :output-dir   "resources/public/js/out"
                                       }}]}
  :figwheel {:css-dirs       ["resources/public/css"]
             :server-logfile "log/figwheel.log"})
