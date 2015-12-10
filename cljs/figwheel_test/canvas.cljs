(ns figwheel-test.canvas
  (:require-macros [figwheel-test.macros :as m])
  (:require [clojure.string :as s]
            [figwheel-test.geometry :as g]))

(defn lines [ctx & [[x0 y0] & _ :as points]]
  (.beginPath ctx)
  (.. ctx (moveTo x0 y0))
  (doseq [[x y] points] (.. ctx (lineTo x y))))

(defn lines-relative [ctx & [[x0 y0 :as p0] & points]]
  (.beginPath ctx)
  (.. ctx (moveTo x0 y0))
  (reduce
   (fn [p pn]
     (let [[x y :as new-p] (g/v+ p pn)]
       (.. ctx (lineTo x y))
       new-p))
   p0 points))

(defn stroke-lines-relative [ctx & args]
  (apply lines-relative ctx args)
  (.stroke ctx))

(defn stroke-lines [ctx & args]
  (apply lines ctx args)
  (.stroke ctx))

(defn stroke-circle [ctx [cx cy] r]
  (.beginPath ctx)
  (.arc ctx cx cy r 0 (* 2 js/Math.PI))
  (.stroke ctx))

(defn stroke-arc [ctx [cx cy] r th1 th2]
  (.beginPath ctx)
  (.arc ctx cx cy r th1 th2)
  (.stroke ctx))

(defn clear [ctx]
  (let [can (.-canvas ctx)]
    (.clearRect ctx 0 0 (.-width can) (.-height can))))

(defn with-saved-context [ctx fun]
  (.save ctx)
  (try (fun)
       (finally (.restore ctx))))

(defn set-stroke [ctx style]
  (set! (.-strokeStyle ctx) style))

(defn elem-offset [elem]
  (loop [e elem
         x (.-offsetLeft elem)
         y (.-offsetTop elem)]
    (if-let [p (.-offsetParent e)]
      (recur p (+ x (.-offsetLeft p)) (+ y (.-offsetTop p)))
      (array x y))))

(defn canvas-coord [ctx evt]
  (let [can (.-canvas ctx)
        [x y] (elem-offset can)]
    (array (- (.-pageX evt) x)
           (- (.-pageY evt) y))))

(defn color-str [components]
  (str (if (= (count components) 4)
         "rgba"
         "rgb")
       "(" (s/join "," components) ")"))

(defn fade-out [rgb frames ctx cont]
  (let [can (.-canvas ctx)
        w (.-width can) h (.-height can)
        alpha (- 1 (js/Math.pow 0.05 (/ 1 frames)))]
    (m/nlet lp [n frames]
            (js/window.requestAnimationFrame
             (fn []
               (with-saved-context ctx
                 (fn []
                   (set! (.-fillStyle ctx)
                         (color-str
                          (if (> n 0)
                            (concat rgb [alpha]) rgb)))
                   (.fillRect ctx 0 0 w h)))
               (if (> n 0) (lp (dec n)) (cont)))))))
