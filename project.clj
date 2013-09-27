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
  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :foreign-libs [{:file "https://maps.googleapis.com/maps/api/js?sensor=false"
                                        :provides ["google.maps"]}]
                        :compiler {:output-to "resources/compiled-cljs.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})

