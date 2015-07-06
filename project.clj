(defproject figwheel-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.rrb-vector "0.0.11"]
                 [hipo "0.4.0"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.7"]]

  :cljsbuild {:builds
              [{:id "foo"
                :source-paths ["cljs/"]
                :figwheel true
                :compiler {:main "figwheel-test.core"
                           :asset-path "js"
                           :output-to "resources/public/stuff.js"
                           :output-dir "resources/public/js"}}

               {:id "bar"
                :optimizations :whitespace
                :source-paths ["cljs/"]
                :figwheel true
                :compiler {:main "figwheel-test.core"
                           :output-to "target/all.js"}}]})
