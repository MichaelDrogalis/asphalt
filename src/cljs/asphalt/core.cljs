(ns asphalt.core
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :refer [chan sliding-buffer timeout >! <!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def transport-chan (chan (sliding-buffer 10)))

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

(def uri "ws://ec2-54-200-49-20.us-west-2.compute.amazonaws.com:9093/asphalt/streaming/edn")

(def ws (js/WebSocket. uri))

(def open-fn
  (fn [] (.log js/console "Connection open. Rock on.")))

(defn plot-coordinates [coordinates]
  (go (let [points (map #(google.maps.LatLng. (:lat %) (:long %)) coordinates)
            new-map (google.maps.visualization.HeatmapLayer. (clj->js {:data points}))
            old-hmap @hmap]
        (swap! hmap (constantly new-map))
        (.setMap new-map gmap)
        (<! (timeout 50))
        (.setMap old-hmap nil))))

(def receive-fn
  (fn [message]
    (go (>! transport-chan (map :coordinates (:snapshot (read-string (.-data message))))))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)

(go (loop []
      (let [coordindates (<! transport-chan)]
        (plot-coordinates coordindates))
      (recur)))

