(ns asphalt.core)

(def uri "ws://localhost:9090")

(def ws (js/$.websocket. ws))

(def open-fn
  (fn [] (.log js/console "Connection open. Rock on.")))

(def receive-fn
  (fn [message]
    (.log js/console "Received message:")
    (.log js/console (:snapshot (read-string (.-data message))))))

(set! (.-onopen ws) open-fn)
(set! (.-onmessage ws) message-fn)

