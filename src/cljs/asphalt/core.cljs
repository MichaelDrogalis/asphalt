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

(defn reverse-links [quad]
  (.log js/console "Linking.")
  (js/$.ajax
   (clj->js
    {:type "POST"
     :url "http://localhost:9091/rush-hour/api/reverse-links/edn"
     :contentType "application/edn"
     :success (fn [response & _] (.log js/console (clj->js (read-string response))))
     :data (pr-str quad)
     :processData false}))
  [])

(defn ingress-pairing [quad driver]
  {:src (first (reverse-links quad))
   :dst (:dst driver)})

(defn egress-pairing [quad driver]
  {:src quad
   :dst (:dst driver)})

(defn ingress-lane-pairs [quad state]
  (map (partial ingress-pairing quad) state))

(defn egress-lane-pairs [quad state]
  (map (partial egress-pairing quad) state))

(defn ingress-all-pairs [ingress-map]
  (into {} (map (fn [[quad state]] {quad (ingress-lane-pairs quad state)}) ingress-map)))

(defn egress-all-pairs [egress-map]
  (into {} (map (fn [[quad state]] {quad (egress-lane-pairs quad state)}) egress-map)))

(def receive-fn
  (fn [message]
    (let [snapshot (:snapshot (read-string (.-data message)))]
;;      (.log js/console "Received message:")
      (clj->js (ingress-all-pairs (:ingress snapshot)))
;;      (.log js/console (clj->js (egress-all-pairs (:egress snapshot))))
      )))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)


