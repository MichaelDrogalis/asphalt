(ns asphalt.core
  (:require [cljs.reader :refer [read-string]]))

(defn by-id [id]
  (.getElementById js/document id))

(defn initialize-map []
  (let [map-options
        {:center (google.maps.LatLng. 39.95005 -75.157066)
         :zoom 16
         :mapTypeId google.maps.MapTypeId/ROADMAP}]
    (google.maps.Map. (by-id "map") (clj->js map-options))))

(.addDomListener google.maps.event js/window "load" initialize-map)

(def uri "ws://localhost:9090/rush-hour/streaming/edn")

(def ws (js/WebSocket. uri))

(def open-fn
  (fn [] (.log js/console "Connection open. Rock on.")))

(defn triangulate-coordinates [driver src dst]
  (.log js/console (:id driver) ": " src "/" dst))

(defn plot-coordinates [coordinates]
  (.log js/console (clj->js coordinates)))

(defn triangulate-egress [resp]
  (let [src (first (vals (first resp)))
        dst (first (vals (second resp)))
        road-length (+ (:street.lane.install/length src)
                       (:street.lane.install/length dst))]
    (js/$.ajax
     (clj->js
      {:type "POST"
       :url "http://localhost:9092/rush-hour/api/triangulate/edn"
       :contentType "application/edn"
       :success (fn [resp & _] (plot-coordinates (:coordinates (read-string resp))))
       :data (pr-str {:src src :dst dst :gap road-length :extender "Philadelphia, PA"})
       :processData false}))))

(defn expand-egress [driver src dst]
  (js/$.ajax
   (clj->js
    {:type "POST"
     :url "http://localhost:9091/rush-hour/api/expand-quad/edn"
     :contentType "application/edn"
     :success (fn [resp & _] (triangulate-egress (:quads (read-string resp))))
     :data (pr-str {:quads [src dst]})
     :processData false})))

(defn parse-src [response]
  (first (:srcs (read-string response))))

(defn ingress-pairing [quad driver]
  (js/$.ajax
   (clj->js
    {:type "POST"
     :url "http://localhost:9091/rush-hour/api/reverse-links/edn"
     :contentType "application/edn"
     :success (fn [resp & _] (triangulate-coordinates driver (parse-src resp) quad))
     :data (pr-str quad)
     :processData false})))

(defn egress-pairing [quad driver]
  (expand-egress driver quad (:dst driver)))

(defn ingress-all-pairs [ingress-map]
  (doseq [[quad state] ingress-map]
    (doseq [driver state]
      (ingress-pairing quad driver))))

(defn egress-all-pairs [egress-map]
  (doseq [[quad state] egress-map]
    (doseq [driver state]
      (egress-pairing quad driver))))

(def receive-fn
  (fn [message]
    (let [snapshot (:snapshot (read-string (.-data message)))]
      (ingress-all-pairs (:ingress snapshot))
      (egress-all-pairs (:egress snapshot)))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)


