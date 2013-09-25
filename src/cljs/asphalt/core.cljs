(ns asphalt.core
  (:require [cljs.reader :refer [read-string]]))

(def uri "ws://localhost:9090/rush-hour/streaming/edn")

(def ws (js/WebSocket. uri))

(def open-fn
  (fn [] (.log js/console "Connection open. Rock on.")))

(def receive-fn
  (fn [message]
    (.log js/console "Received message:")
    (.log js/console (clj->js (:snapshot (read-string (.-data message)))))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) receive-fn)

