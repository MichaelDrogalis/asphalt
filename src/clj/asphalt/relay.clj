(ns asphalt.relay
  (:require [clojure.core.async :refer [chan go >! <!]]
            [aleph.http :refer [websocket-client]]
            [lamina.core :refer [wait-for-result receive-all]]
            [clj-http.client :as client])
  (:import [org.webbitserver WebServer WebServers WebSocketHandler]))

(def trans-chan (chan 10000))

(def config (read-string (slurp (clojure.java.io/resource "config.edn"))))

(def reverse-links
  (memoize
   (fn [quad]
     (read-string
      (:body
       (client/post (:reverse-links-url config)
                    {:body (pr-str quad)
                     :content-type "application/edn"}))))))

(def quad-expansion
  (memoize
   (fn [quad]
     (read-string
      (:body
       (client/post (:quad-expansion-url config)
                    {:body (pr-str quad)
                     :content-type "application/edn"}))))))

(def triangulate
  (memoize
   (fn [src dst gap]
     (read-string
      (:body
       (client/post (:triangulation-url config)
                    {:body (pr-str {:src src :dst dst :gap gap :extender "Philadelphia, PA"})
                     :content-type "application/edn"}))))))

(defn triangulate-ingress [driver src dst]
  (let [gap (min (:street.lane.install/length src) (:front driver))]
    (triangulate src dst gap)))

(defn triangulate-egress [driver src dst]
  (let [gap (min (+ (:street.lane.install/length src) (:street.lane.install/length dst))
                 (+ (:street.lane.install/length dst) (:front driver)))]
    (triangulate src dst gap)))

(defn ingress-coordindates [payload]
  (filter not-empty
          (mapcat
           (fn [[quad state]]
             (map
              (fn [driver]
                (triangulate-ingress
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
                (triangulate-egress
                 driver
                 (first (vals (quad-expansion quad)))
                 (first (vals (quad-expansion (:dst driver))))))
              state))
           payload)))

(def sim-snapshot (atom nil))

(def listeners (atom #{}))

(def ws-chan (websocket-client {:url (:web-socket-channel config)}))

(defn receive [snapshot]
  (swap! sim-snapshot
         (constantly (concat (ingress-coordindates (:ingress snapshot))
                             (egress-coordinates (:egress snapshot))))))

(receive-all (wait-for-result ws-chan) #(receive (:snapshot (read-string %))))

(defn push-to-clients [ss]
  (doseq [channel @listeners]
    (.send channel (pr-str {:snapshot ss}))))

(add-watch sim-snapshot :socket
           (fn [_ _ _ sim-ss]
             (push-to-clients sim-ss)))

(defn -main [& args]
  (doto (WebServers/createWebServer 9093)
    (.add "/asphalt/streaming/edn"
          (proxy [WebSocketHandler] []
            (onOpen [chan] (swap! listeners conj chan))
            (onClose [chan] (swap! listeners disj chan))
            (onMessage [_])))
    (.start)))

