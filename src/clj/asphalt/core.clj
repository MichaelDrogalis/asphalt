(ns asphalt.core
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]))

(def view
  (html
   [:html
    (include-js "http://code.jquery.com/jquery-latest.min.js")
    (include-js "/resources/main.js")
    [:body]]))

