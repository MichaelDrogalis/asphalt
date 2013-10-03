(ns asphalt.core
  (:require [cljs.reader :refer [read-string]]))

(defn by-id [id]
  (.getElementById js/document id))

(defn initialize-map []
  (let [map-options
        {:center (google.maps.LatLng. 39.95005 -75.157066)
         :zoom 16
         :mapTypeId google.maps.MapTypeId/ROADMAP}]
    (def gmap (google.maps.Map. (by-id "map") (clj->js map-options)))))

(.addDomListener google.maps.event js/window "load" initialize-map)

(def uri "ws://localhost:9090/rush-hour/streaming/edn")

(def ws (js/WebSocket. uri))

(def open-fn
  (fn [] (.log js/console "Connection open. Rock on.")))

(defn plot-coordinates [{:keys [lat long]}]
  (let [points (google.maps.MVCArray. (clj->js [(google.maps.LatLng. lat long)]))
        heat-map (google.maps.visualization.HeatmapLayer. (clj->js {:data points}))]
    (.setMap heat-map gmap)))

(def receive-fn
  (fn [message]
    (read-string (.-data message))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)


