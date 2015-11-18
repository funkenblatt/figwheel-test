(ns figwheel-test.common
  (:require [hipo.core :as hipo]
            [figwheel-test.canvas :as c]
            goog.object
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
            (.setTransform ctx s 0 0 (- s) (* 640 s) (* 480 s))
            (f)))))

(def mobile? false)

(defn undo-viewport [[x y]]
  [(- (/ x (scale-factor)) 640) (+ 480 (- (/ y (scale-factor))))])

(defn on-space [f]
  (set! js/window.onkeypress
        (fn [e]
          (when (= (.-which e) 32)
            (f))))

  (set! canvas.ontouchstart
        (fn [e]
          (set! mobile? true)
          (when (-> (c/canvas-coord ctx e)
                    undo-viewport
                    first
                    js/Math.abs
                    (< 200))
            (f)))))

(defn viewport-print-1 [x y txt {:keys [align font]
                                 :or {align :left
                                      font "20px sans"}}]
  (c/with-saved-context ctx
    (fn []
      (set! (.-font ctx) font)
      (set! (.-fillStyle ctx) "#000")
      (let [width (-> ctx (.measureText txt) .-width)
            x-offset (case align
                       :left 0
                       :center (/ width 2)
                       :right width)]
        (.translate ctx (- x x-offset) y)
        (.scale ctx 1 -1)
        (.fillText ctx txt 0 0)))))

(defn viewport-print
  "Print something on the canvas, assuming usual viewport coordinate
  system.  Takes an optional alignment argument that can be one of
  :left, :right, or :center"
  ([x y txt & {:as options}]
   (with-viewport
     #(->> (clojure.string/split txt "\n")
           (map clojure.string/trim)
           (reduce (fn [y s]
                     (viewport-print-1 x y s options)
                     (- y 20))
                   y))
     false)))

(defn center-print [s]
  (viewport-print 0 0 s :align :center))
