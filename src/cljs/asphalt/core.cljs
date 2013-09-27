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

(defn log-pairs [driver src dst]
  (.log js/console (:id driver) ": " (clj->js src) "/" (clj->js dst)))

(defn parse-src [response]
  (first (:srcs (read-string response))))

(defn ingress-pairing [quad driver]
  (js/$.ajax
   (clj->js
    {:type "POST"
     :url "http://localhost:9091/rush-hour/api/reverse-links/edn"
     :contentType "application/edn"
     :success (fn [resp & _] (log-pairs driver (parse-src resp) quad))
     :data (pr-str quad)
     :processData false})))

(defn egress-pairing [quad driver]
  (log-pairs driver quad (:dst driver)))

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


