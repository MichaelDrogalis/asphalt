(defproject asphalt "0.1.0-SNAPSHOT"
  :description "Heat map for Rush Hour streaming API"
  :url "https://github.com/MichaelDrogalis/asphalt"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojars.dsabanin/webbit "0.4.16"]
                 [aleph "0.3.0"]
                 [lamina "0.5.0"]
                 [clj-http "0.7.7"]]
  :plugins [[lein-cljsbuild "0.3.3"]]
  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :foreign-libs [{:file "https://maps.googleapis.com/maps/api/js?sensor=false"
                                        :provides ["google.maps"]}]
                        :compiler {:output-to "resources/compiled-cljs.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :main asphalt.relay)

