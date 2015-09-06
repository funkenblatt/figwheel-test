(ns figwheel-test.common
  (:require [hipo.core :as hipo]))

(def tau (* 2 js/Math.PI))
(def button (hipo/create [:button "Pause"])) 
(def canvas (let [w (min 1280
                         (quot (* (- js/window.innerWidth 20) 3) 4)
                         (quot (* (- js/window.innerHeight 10) 4) 3))
                  h (quot (* w 3) 4)]
              
              (hipo/create [:canvas {:width w :height h
                                     :style "border: 1px solid #000; display: block;"}])))

(def print-area (hipo/create [:div]))
(def ctx (.getContext canvas "2d"))

(defn fooprint [& args]
  (set! (.-textContent print-area)
        (->> (map str args) (clojure.string/join ""))))

(defn ^:export init-elements []
  (let [body (js/document.querySelector "body")]
    (set! (.-innerHTML body) "")
    (.appendChild body (doto (hipo/create
                              [:div {:style "float: right; text-align: right; width: 25%"}])
                         (.appendChild button)
                         (.appendChild print-area)))
    (.appendChild body canvas)))
