(ns asphalt.relay
  (:require [clojure.core.async :refer [chan go >! <!]]
            [aleph.http :refer [websocket-client]]
            [lamina.core :refer [wait-for-result receive-all]]
            [clj-http.client :as client]))

(def trans-chan (chan 10000))

(defn reverse-links [quad]
  (read-string
   (:body
    (client/post "http://localhost:9091/rush-hour/api/reverse-links/edn"
                 {:body (pr-str quad)
                  :content-type "application/edn"}))))

(defn quad-expansion [quad]
  (read-string
   (:body
    (client/post "http://localhost:9091/rush-hour/api/expand-quad/edn"
                 {:body (pr-str quad)
                  :content-type "application/edn"}))))

(defn triangulate [src dst gap]
  (read-string
   (:body
    (client/post "http://localhost:9092/rush-hour/api/triangulate/edn"
                 {:body (pr-str {:src src :dst dst :gap gap :extender "Philadelphia, PA"})
                  :content-type "application/edn"}))))

(defn triangulate-lane [driver src dst]
  (let [road-length (+ (:street.lane.install/length src)
                       (:street.lane.install/length dst))
        gap (- road-length (:front driver))]
    (triangulate src dst gap)))

(defn ingress-coordindates [payload]
  (filter not-empty
          (mapcat
           (fn [[quad state]]
             (map
              (fn [driver]
                (triangulate-lane
                 driver
                 (first (vals (quad-expansion (first (:srcs (reverse-links quad))))))
                 (first (vals (quad-expansion quad)))))
              state))
           payload)))

(defn egress-coordinates [payload]
  (filter not-empty
          (mapcat
           (fn [[quad state]]
             (map
              (fn [driver]
                (triangulate-lane
                 driver
                 (first (vals (quad-expansion quad)))
                 (first (vals (quad-expansion (:dst driver))))))
              state))
           payload)))

(def ws-chan (websocket-client {:url "http://localhost:9090/rush-hour/streaming/edn"}))

(defn receive [snapshot]
  (concat (ingress-coordindates (:ingress snapshot))
          (egress-coordinates (:egress snapshot))))

(receive-all (wait-for-result ws-chan) #(receive (:snapshot (read-string %))))
