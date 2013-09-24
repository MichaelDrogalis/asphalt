(defproject asphalt "0.1.0-SNAPSHOT"
  :description "Heat map for Rush Hour streaming API"
  :url "https://github.com/MichaelDrogalis/asphalt"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [hiccup "1.0.2"]
                 [hiccups "0.2.0"]]
  :plugins [[lein-cljsbuild "0.3.3"]]
  :source-paths ["src/clj"]
  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :compiler {:output-to "resources/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})

