(ns asphalt.core
  (:require [cljs.reader :refer [read-string]]))

(def hmap (atom nil))

(defn by-id [id]
  (.getElementById js/document id))

(defn initialize-map []
  (let [map-options
        {:center (google.maps.LatLng. 39.95005 -75.157066)
         :zoom 16
         :mapTypeId google.maps.MapTypeId/ROADMAP}]
    (def gmap (google.maps.Map. (by-id "map") (clj->js map-options)))))

(.addDomListener google.maps.event js/window "load" initialize-map)

(def uri "ws://localhost:9093/asphalt/streaming/edn")

(def ws (js/WebSocket. uri))

(def open-fn
  (fn [] (.log js/console "Connection open. Rock on.")))

(defn plot-coordinates [coordinates]
  (let [points (clj->js (map (fn [x] [(:lat x) (:long x)]) coordinates))
        _ (.log js/console points)
        heat-map (google.maps.visualization.HeatmapLayer. (clj->js {:data points}))]
    (.setMap @hmap nil)
    (swap! hmap (constantly heat-map))
    (.setMap heat-map gmap)))

(def receive-fn
  (fn [message]
    (.log js/console (clj->js (map first (:snapshot (read-string (.-data message))))))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)


