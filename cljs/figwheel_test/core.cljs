(ns figwheel-test.core
  (:require [hipo.core :as hipo]
            [figwheel-test.geometry :as g]
            [figwheel-test.canvas :as c]))

(set! (.-innerHTML (js/document.querySelector "body")) "")

(def canvas (hipo/create [:canvas {:width 1280 :height 960
                                   :style "border: 1px solid #000"}]))

(def ctx (.getContext canvas "2d"))

(.appendChild (js/document.querySelector "body") canvas)

(def l1 [[0 0] [100 100]])
(def l2 [[40 23] [50 -10]])

(defn stroke-poly [ctx polyline]
  (apply c/stroke-lines ctx polyline))

(def my-poly [[0 0] [100 0] [150 100] [100 200] [0 200] [0 0]])

(defn perspective-transform [mult]
  (fn [[x y z]] [(/ (* x mult) z) (/ (* y mult) z)]))

(def my-transform (perspective-transform 300))

(defn draw-block
  "Draw an axis-aligned 3d box thing.  Corners should be such that
ax < bx, ay > by.  Z represents the camera's height off the ground."
  [ctx [ax ay] [bx by] height z]
  (let [z z
        top-z (- z height)
        top [[ax ay top-z] [bx ay top-z] [bx by top-z] [ax by top-z] [ax ay top-z]]
        front [[ax by top-z] [ax by z] [bx by z] [bx by top-z]]
        left [[ax ay top-z] [ax ay z] [ax by z] [ax by top-z]]
        right [[bx ay top-z] [bx ay z] [bx by z] [bx by top-z]]
        back [[ax ay top-z] [ax ay z] [bx ay z] [bx ay top-z]]]
    
    (stroke-poly ctx (map my-transform top))

    (if (and (> ay 0) (> by 0))
      (stroke-poly ctx (map my-transform front)))

    (if (and (> ax 0) (> bx 0))
      (stroke-poly ctx (map my-transform left)))
    
    (if (and (< ax 0) (< bx 0))
      (stroke-poly ctx (map my-transform right)))

    (if (and (< ay 0) (< by 0))
      (stroke-poly ctx (map my-transform back)))))

(def l3 [[40 -20] [0 -4]])

(def blocks [[[40 350] [140 250] 20]
             [[-300 250] [-200 150] 5]
             [[-200 -50] [-100 -150] 30]
             [[200 -100] [300 -150] 15]
             [[430 -397] [448 -495] 8]
             [[-408 81] [-401 -18] 7]
             [[-311 (+ 200 278)] [-270 (+ 200 203)] 11]])

(defn random-block []
  (let [x (- (rand-int 1000) 500)
        y (- (rand-int 1000) 500)]
    [[x y] [(+ x (rand-int 100))
            (- y (rand-int 100))]
     (+ (rand-int 25) 5)]))

(defn draw-scene [offset]
  (c/with-saved-context
    ctx
    (fn []
      (c/clear ctx)
      (doto ctx (.translate 640 480) (.scale 1 -1))
      (doseq [[c1 c2 height] blocks]
        (draw-block ctx (g/v+ c1 offset) (g/v+ c2 offset) height 300)))))

(def state (atom [0 0]))

(defn setup []
  (draw-scene @state)
  (set! (.-onmousedown canvas)
        (fn [evt]
          (let [startx (.-pageX evt)
                starty (.-pageY evt)]
            (set! (.-onmousemove js/window)
                  (fn [move]
                    (let [x (.-pageX move)
                          y (.-pageY move)]
                      (.requestAnimationFrame
                       js/window
                       #(draw-scene
                         (g/v+ @state [(- x startx) (- starty y)]))))))
            (set! (.-onmouseup js/window)
                  (fn [finish]
                    (set! (.-onmousemove js/window) nil)
                    (set! (.-onmouseup js/window) nil)
                    (swap! state g/v+ [(- (.-pageX finish) startx)
                                       (- starty (.-pageY finish))])
                    (.requestAnimationFrame
                     js/window #(draw-scene @state))))))))
