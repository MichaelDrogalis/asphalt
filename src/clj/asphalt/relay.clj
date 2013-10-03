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

;;; (def ws-chan (websocket-client {:url "http://localhost:9090/rush-hour/streaming/edn"}))

(defn receive [snapshot]
  (go (>! trans-chan snapshot)))

(comment
  (receive-all (wait-for-result ws-chan) #(receive (:snapshot (read-string %))))

  (while true
    (go (let [x (<! trans-chan)]
          (clojure.pprint/pprint x)))))

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

(def data
  '{:lights
    {["10th Street" "Chestnut Street"] {?x [:green]},
     ["9th Street" "Chestnut Street"] {?x [:green]},
     ["9th Street" "Market Street"] {?x [:green]},
     ["10th Street" "Market Street"] {?x [:green]}},
    :ingress
    {{:lane/name "in",
      :street/tag "north",
      :street/name "10th Street",
      :intersection/of ["10th Street" "Chestnut Street"]}
     [],
     {:lane/name "in",
      :street/tag "west",
      :street/name "Chestnut Street",
      :intersection/of ["9th Street" "Chestnut Street"]}
     [{:ripe? false,
       :dst
       {:intersection/of ["10th Street" "Chestnut Street"],
        :street/name "Chestnut Street",
        :street/tag "east",
        :lane/name "out"},
       :front 65,
       :len 1,
       :id "Mike",
       :buf 0}],
     {:lane/name "in",
      :street/tag "south",
      :street/name "9th Street",
      :intersection/of ["9th Street" "Market Street"]}
     [],
     {:lane/name "in",
      :street/tag "east",
      :street/name "Market Street",
      :intersection/of ["10th Street" "Market Street"]}
     []},
    :egress
    {{:lane/name "out",
      :street/tag "east",
      :street/name "Chestnut Street",
      :intersection/of ["10th Street" "Chestnut Street"]}
     [],
     {:lane/name "out",
      :street/tag "north",
      :street/name "9th Street",
      :intersection/of ["9th Street" "Chestnut Street"]}
     [],
     {:lane/name "out",
      :street/tag "west",
      :street/name "Market Street",
      :intersection/of ["9th Street" "Market Street"]}
     [],
     {:lane/name "out",
      :street/tag "south",
      :street/name "10th Street",
      :intersection/of ["10th Street" "Market Street"]}
     []}})

