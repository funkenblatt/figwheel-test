(ns figwheel-test.platform
  (:require [figwheel-test.geometry :as g]
            [figwheel-test.canvas :as c]
            [figwheel-test.snake-levels :as l]
            [figwheel-test.common :refer [tau canvas ctx fooprint
                                          init-elements scale-factor
                                          with-viewport center-print
                                          on-space undo-viewport
                                          mobile?]
             :as com]
            [clojure.core.rrb-vector :as rrb]
            [hipo.core :as hipo]
            clojure.string
            [figwheel-test.stick :refer [update-walk walk-frames draw-figure
                                         show-throw]
             :as stick])
  (:require-macros [figwheel-test.macros :as m]))

(def world {:player {:x [0 0.001]
                     :v [0 0]
                     :walk-state {:limb-state (first walk-frames)
                                  :phase 0
                                  :crouched 0
                                  :x 0
                                  :dir -1
                                  :throw? false}}
            :walls [[[-640 -300] [-640 100]]
                    [[-640 -300] [0 -300]]
                    [[0 -300] [0 0]]
                    [[0 0] [640 0]]
                    [[-200 -250] [-100 -250]]
                    [[-350 -200] [-250 -200]]
                    [[-500 -150] [-400 -150]]]
            :balls {}})

(defn draw-world [{:as world
                   :keys [walls player balls]}]
  (with-viewport
    (fn []
      (c/with-saved-context
        com/ctx
        (fn []
          (set! (.-lineWidth com/ctx) 2)
          (run! (fn [points] (apply c/stroke-lines com/ctx points)) walls)
          (run! (fn [[k {:keys [x]}]] (c/stroke-circle com/ctx x 2)) balls)))
      (draw-figure com/ctx (get-in player [:walk-state :limb-state])
                   :offset (g/v+ (:x player) [0 101.96249999999999])
                   :dir (get-in player [:walk-state :dir])))
    true))

(defn resting-on [x [w1 w2 :as wall]]
  (let [[wx wy] (g/v- w2 w1)
        norm (g/normalized [(- wy) wx])
        nd (g/vdot norm (g/v- x w1))]
    (and (<= (js/Math.abs nd) 0.002)
         (> (* (g/vdot norm [0 1]) nd) 0)
         wall)))

(def key-force-map {65 -800 68 800})

(def dt (/ 1 60))

(defn box-line-ccd [[[c1x c1y] [c2x c2y] :as rect] v 
                    [[p1x p1y :as p1] [p2x p2y :as p2] :as seg]]
  (let [[m1x m1y :as m1] [(- (min p2x p1x) (max c1x c2x))
                          (- (max p2y p1y) (min c1y c2y))]
        [m2x m2y :as m2] [(- (max p2x p1x) (min c1x c2x))
                          (- (min p2y p1y) (max c1y c2y))]]
    (reduce
     (fn [[m-edge s :as acc] [p1 p2 :as l]]
       (let [new-s (g/ray-intersection [0 0] v l)]
         (if (and (or (not s) (< new-s s)) (> new-s 0))
           [l new-s]
           acc)))
     nil [[[m1x m1y] [m2x m1y]]
          [[m2x m1y] [m2x m2y]]
          [[m2x m2y] [m1x m2y]]
          [[m1x m2y] [m1x m1y]]])))

(defn wall-hit? [rect v wall]
  (let [[[p1 p2] s] (or (box-line-ccd rect v wall)
                        [nil -1])]
    (if (>= s 0)
      (let [[dx dy] (g/v- p2 p1)
            norm (g/normalized [(- dy) dx])]
        [(- s (/ 0.001 (js/Math.abs (g/vdot v norm)))) norm])
      nil)))

(defn do-wall-collisions [x v walls restitution rect]
  (let [xform (fn [x dx]
                (let [r (rect x)]
                  (comp (map (fn [wall] (wall-hit? r dx wall)))
                        (filter identity))))
        rf (partial min-key first)
        init [1 nil]
        dx (g/vscale dt v)]
    (loop [x x t 0
           [s norm] (transduce (xform x dx) rf init walls)
           v v]
      (if (and (< t 1) norm)
        (let [x (g/v+ x (g/vscale (* dt (- 1 t) s) v))
              t (min 1 (+ t s))
              v (g/v- v (g/vscale 
                         (* (+ 1 restitution) (g/vdot norm v))
                         norm))
              dx (g/vscale (* dt (- 1 t)) v)]
          (recur x
                 t
                 (transduce (xform x dx) rf init walls)
                 v))
        [(g/v+ x (g/vscale (- dt (* dt t)) v)) v]))))

(defn tangent-v
  "Enforces that the horizontal velocity component is correct based on
the currently pressed keys."
  [v keys]
  [(transduce (map (some-fn {65 -150 68 150} (constantly 0))) + keys)
   (nth v 1)])

(def omega (js/Math.sqrt 0.4))

(def counter (atom 0))

(defn clamp-v 
  "Clamp vector v to some maximum magnitude."
  [v max-mag]
  (let [m (g/vmag v)]
    (if (> m max-mag)
      (g/vscale (/ max-mag m) v)
      v)))

(defn spawn-ball [{:as world {:as player :keys [x v walk-state]} :player}
                  pointer]
  (update world :balls assoc (swap! counter com/modinc 5)
          {:x (stick/throw-origin walk-state x)
           :v (g/v+ v
                    (clamp-v
                     (g/vscale 4 (g/v- pointer (stick/throw-origin walk-state x)))
                     1000))}))

(defn coriolis-force [x v]
  (g/v+ (g/vscale (* omega omega) (g/v- x [0 2000]))
        (let [[vx vy] v]
          [(* 2 vy omega) (* -2 vx omega)])))

(defn update-ball [{:as world :keys [walls]} k {:keys [x v]}]
  (let [force (g/v+ (coriolis-force x v) (g/vscale -0.1 v))
        new-v (g/v+ v (g/vscale dt force))
        [new-x new-v] (do-wall-collisions 
                       x new-v walls 0.5
                       (fn [p] [(g/v+ p [-2 2])
                                (g/v+ p [2 -2])]))]
    (assoc-in world [:balls k] {:x new-x :v new-v})))

(defn update-all [state key update-fn]
  (reduce-kv update-fn state (key state)))

(defn maybe-add-ball [{:as world 
                       {{:keys [throw?]} :walk-state} :player}
                      pointer]
  (if (= throw? 10) (spawn-ball world pointer) world))

(defn update-world [{:as world
                     :keys [walls player]}
                    keys pointer]
  (let [{player-x :x player-v :v} player
        grounded? (some (partial resting-on player-x) walls)
        coriolis (coriolis-force player-x player-v)
        player-force (g/v+ coriolis (g/vscale -0.1 player-v))        
        jump-impulse (if (and grounded? (keys 32)) [0 400] [0 0])
        new-v (g/v+ player-v (g/v+ (g/vscale dt player-force) jump-impulse))
        new-v (if grounded? (tangent-v new-v keys) new-v)
        [new-x new-v] (do-wall-collisions player-x new-v walls 0
                                          (fn [p] 
                                            [(g/v+ p [-30 0])
                                             (g/v+ p [30 30])]))]
    (-> (assoc-in world [:player :v] new-v)
        (assoc-in [:player :x] new-x)
        (update-in [:player :walk-state] update-walk keys)
        (update :player show-throw pointer)
        (update-all :balls update-ball)
        (maybe-add-ball pointer))))

(def down-keys (atom #{}))

(def mouse-loc (atom nil))

(def stop false)

(def worldwad (atom world))

(defn run-stuff []
  (set! js/window.onkeydown 
        (fn [e] (swap! down-keys conj (.-which e)) (.preventDefault e)))
  (set! js/window.onkeyup 
        (fn [e] (swap! down-keys disj (.-which e)) (.preventDefault e)))
  (set! (.-onmousedown com/canvas) 
        (fn [e] 
          (swap! down-keys conj :mouse)
          (let [f #(->> % (c/canvas-coord com/ctx) undo-viewport (reset! mouse-loc))]
            (f e)
            (set! js/window.onmousemove f))))
  (set! (.-onmouseup js/window) 
        (fn [e] 
          (swap! down-keys disj :mouse)
          (set! js/window.onmousemove nil)))

  (set! stop false)
  (m/nlet lp []
    (when (not stop)
      (draw-world @worldwad)
      (swap! worldwad update-world @down-keys @mouse-loc)
      (js/window.requestAnimationFrame lp))))

(comment
  (run-stuff)

  (add-watch down-keys :foo (fn [k r o n] (set! js/document.title (str n))))
  
  (set! stop true)
  (reset! worldwad world))
