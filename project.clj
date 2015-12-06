(defproject figwheel-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.rrb-vector "0.0.11"]
                 [hipo "0.4.0"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.7"
             :exclusions [org.clojure/clojure
                          org.clojure/core.async
                          org.clojure/clojurescript
                          org.clojure/tools.reader
                          org.codehaus.plexus/plexus-utils]]]

  :profiles {:dev {:repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :dependencies [[weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                                  [com.cemerick/piggieback "0.2.1" :exclusions [org.clojure/clojurescript]]
                                  [figwheel "0.3.7"
                                   :exclusions
                                   [org.clojure/clojure org.clojure/clojurescript
                                    org.clojure/tools.reader org.codehaus.plexus/plexus-utils]]
                                  [figwheel-sidecar "0.5.0-1"]
                                  [org.clojure/tools.nrepl "0.2.10"]]
                   :source-paths ["cljs/"]}}

  :cljsbuild {:builds
              [{:id "foo"
                :source-paths ["cljs/" "src/"]
                :figwheel {:websocket-host :js-client-host}
                :compiler {:main "figwheel-test.snake"
                           :asset-path "js"
                           :output-to "resources/public/stuff.js"
                           :output-dir "resources/public/js"}}

               {:id "production"
                :source-paths ["cljs/"]
                :compiler {:optimizations :advanced
                           :main "figwheel-test.snake"
                           :output-to "target/all.js"
                           }}

               {:id "turret"
                :source-paths ["cljs/" "src/"]
                :compiler {:optimizations :advanced
                           :main "figwheel-test.turret"
                           :output-to "target/turret.js"
                           }}]})
