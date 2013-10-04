(ns asphalt.core
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [timeout !<]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def hmap (atom (google.maps.visualization.HeatmapLayer. (clj->js {:data []}))))

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

(defn fade-out-map [heat-map]
  (go (loop [opacity 0.60]
        (when (pos? opacity)
          (.setOptions heat-map (clj->js {:opacity opacity}))
          (<! (timeout 10))
          (recur (- opacity 0.05))))
      (.setMap heat-map nil)))

(defn plot-coordinates [coordinates]
  (let [points (map #(google.maps.LatLng. (:lat %) (:long %)) coordinates)
        new-map (google.maps.visualization.HeatmapLayer. (clj->js {:data points}))
        old-hmap @hmap]
    (swap! hmap (constantly new-map))
    (.setMap new-map gmap)
    (fade-out-map old-hmap)))

(def receive-fn
  (fn [message]
    (plot-coordinates (map :coordinates (:snapshot (read-string (.-data message)))))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)


