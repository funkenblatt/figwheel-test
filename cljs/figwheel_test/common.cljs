(ns figwheel-test.common
  (:require [hipo.core :as hipo]
            [figwheel-test.canvas :as c]
            goog.object
            clojure.string))

(def tau (* 2 js/Math.PI))

(def canvas (hipo/create [:canvas
                          {:width (min js/window.innerWidth 1280)
                           :height (min js/window.innerHeight 960)
                           :style "border: 1px solid #000; display: block;"}]))

(set! js/window.onresize
      (fn []
        (set! (.-width canvas) (min js/window.innerWidth 1280))
        (set! (.-height canvas) (min js/window.innerHeight 960))))

(defn scale-factor []
  (let [aspect-ratio (/ (.-width canvas)
                        (.-height canvas))]
    (if (< aspect-ratio (/ 4 3))
      (-> canvas .-height (/ 960))
      (-> canvas .-width (/ 1280)))))

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
          (let [s 1]
            (.setTransform ctx s 0 0 (- s)
                           (/ (.-width canvas) 2)
                           (/ (.-height canvas) 2))
            (f)))))

(def mobile? false)

(defn undo-viewport [[x y]]
  [(- x (/ (.-width canvas) 2)) (- (/ (.-height canvas) 2) y)])

(defn on-space [f]
  (set! js/window.onkeypress
        (fn [e]
          (when (= (.-which e) 32)
            (f))))

  (set! canvas.ontouchstart
        (fn [e]
          (set! mobile? true)
          (when (-> (c/canvas-coord ctx (-> e .-touches (.item 0)))
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
  ([x y txt & {:as options
               :keys [line-height]
               :or {line-height 20}}]
   (with-viewport
     #(->> (clojure.string/split txt "\n")
           (map clojure.string/trim)
           (reduce (fn [y s]
                     (viewport-print-1 x y s options)
                     (- y line-height))
                   y))
     false)))

(defn text-width [font text]
  (c/with-saved-context
    ctx
    (fn []
      (set! (.-font ctx) font)
      (-> ctx (.measureText text) .-width))))

(defn center-print [s]
  (viewport-print 0 0 s :align :center))
