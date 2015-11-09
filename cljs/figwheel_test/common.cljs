(ns figwheel-test.common
  (:require [hipo.core :as hipo]
            [figwheel-test.canvas :as c]
            clojure.string))

(def tau (* 2 js/Math.PI)) 
(def canvas (let [w (min 1280
                         (quot (* (- js/window.innerWidth 20) 3) 4)
                         (quot (* (- js/window.innerHeight 10) 4) 3))
                  h (quot (* w 3) 4)]
              
              (hipo/create [:canvas {:width w :height h
                                     :style "border: 1px solid #000; display: block;"}])))

(defn scale-factor []
  (-> canvas .-width (/ 1280)))

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
                         (.appendChild print-area)))
    (.appendChild body canvas)))

(defn with-viewport [f clear?]
  (c/with-saved-context
    ctx (fn []
          (when clear? (c/clear ctx))
          (let [s (scale-factor)]
            (.scale ctx s s)
            (.translate ctx 640 480)
            (.scale ctx 1 -1)
            (f)))))

(defn on-space [f]
  (set! js/window.onkeypress
        (fn [e]
          (when (= (.-which e) 32)
            (f))))

  (set! canvas.ontouchstart
        (fn [e] (f))))

(defn center-print [s]
  (with-viewport
    (fn []
      (set! (.-font ctx) "20px sans")
      (.scale ctx 1 -1)
      (run!
       (fn [x]
         (.fillText ctx x
                    (- (/ (.-width (.measureText ctx x)) 2))
                    0)
         (.translate ctx 0 20))
       (map clojure.string/trim
            (clojure.string/split s "\n"))))
    false))

(defn undo-viewport [[x y]]
  [(- (/ x (scale-factor)) 640) (+ 480 (- (/ y (scale-factor))))])
