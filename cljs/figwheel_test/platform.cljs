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
            [figwheel-test.stick :refer [update-walk walk-frames draw-figure]])
  (:require-macros [figwheel-test.macros :as m]))

(def world {:player {:x [0 0.001]
                     :v [0 0]
                     :walk-state {:limb-state (first walk-frames)
                                  :phase 0
                                  :crouched 0
                                  :x 0
                                  :dir -1}}
            :walls [[[-640 -300] [-640 100]]
                    [[-640 -300] [0 -300]]
                    [[0 -300] [0 0]]
                    [[0 0] [640 0]]
                    [[-200 -250] [-100 -250]]
                    [[-350 -200] [-250 -200]]
                    [[-500 -150] [-400 -150]]]})

(defn draw-world [{:as world
                   :keys [walls player]}]
  (with-viewport
    (fn []
      (run! (fn [points] (apply c/stroke-lines com/ctx points)) walls)
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

(defn wall-hit? [p v wall]
  (let [[[p1 p2] s] (or (box-line-ccd [(g/v+ p [-30 0])
                                       (g/v+ p [30 30])]
                                      v wall)
                        [nil -1])]
    (if (>= s 0)
      (let [[dx dy] (g/v- p2 p1)
            norm (g/normalized [(- dy) dx])]
        [(- s (/ 0.001 (js/Math.abs (g/vdot v norm)))) norm])
      nil)))

(defn do-wall-collisions [x v walls]
  (let [xform (fn [x dx]
                (comp (map (fn [wall] (wall-hit? x dx wall)))
                      (filter identity)))
        rf (partial min-key first)
        init [1 nil]
        dx (g/vscale dt v)]
    (loop [x x t 0
           [s norm] (transduce (xform x dx) rf init walls)
           v v]
      (if (and (< t 1) norm)
        (let [x (g/v+ x (g/vscale (* dt (- 1 t) s) v))
              t (+ t s)
              v (g/v- v (g/vscale (g/vdot norm v) norm))
              dx (g/vscale (* dt (- 1 t)) v)]
          (recur x
                 t
                 (transduce (xform x dx) rf init walls)
                 v))
        [(g/v+ x (g/vscale (- dt (* dt t)) v)) v]))))

(defn foot-force 
  "Provide the horizontal force for player motion.  Falls off fairly quickly with velocity."
  [keys [vx vy :as v]]
  (if (some key-force-map keys)
    (reduce ((map (fn [k] 
                    (let [fx (get key-force-map k 0)]
                      (if (> (* fx vx) 0)
                        (if (> fx 0) 
                          (- fx (min fx (* .035 vx vx)))
                          (+ fx (min (- fx) (* .035 vx vx))))
                        fx)))) +)
            0 keys)
    (* -8 vx)))

(defn tangent-v
  "Enforces that the horizontal velocity component is correct based on
the currently pressed keys."
  [v keys]
  [(transduce (map (some-fn {65 -150 68 150} (constantly 0))) + keys)
   (nth v 1)])

(def omega (js/Math.sqrt 0.4))

(defn update-world [{:as world
                     :keys [walls player]}
                    keys]
  (let [{player-x :x player-v :v} player
        grounded? (some (partial resting-on player-x) walls)
        coriolis (g/v+ (g/vscale (* omega omega) (g/v- player-x [0 2000]))
                       (let [[vx vy] player-v]
                         [(* 2 vy omega) (* -2 vx omega)]))
        player-force (g/v+ coriolis (g/vscale -0.1 player-v))        
        jump-impulse (if (and grounded? (keys 32)) [0 400] [0 0])
        new-v (g/v+ player-v (g/v+ (g/vscale dt player-force) jump-impulse))
        new-v (if grounded? (tangent-v new-v keys) new-v)
        [new-x new-v] (do-wall-collisions player-x new-v walls)]
    (-> (assoc-in world [:player :v] new-v)
        (assoc-in [:player :x] new-x)
        (update-in [:player :walk-state] update-walk keys))))

(def down-keys (atom #{}))

(comment
  (set! js/window.onkeydown (fn [e] (swap! down-keys conj (.-which e)) (.preventDefault e)))
  (set! js/window.onkeyup (fn [e] (swap! down-keys disj (.-which e)) (.preventDefault e)))

  (add-watch down-keys :foo (fn [k r o n] (set! js/document.title (str n))))
  (def stop false)
  (set! stop true)

  (def worldwad (atom world))
  (reset! worldwad world)
  (do
    (set! stop false)
    (m/nlet lp []
      (when (not stop)
        (draw-world @worldwad)
        (swap! worldwad update-world @down-keys)
        (js/window.requestAnimationFrame lp))))

  (cps-run!
   (fn [x k]
     (draw-world (update-in @worldwad [:player :x] g/v- [(* 2.5 x) 0]))
     (js/window.requestAnimationFrame k))
   (range 60))

  (:x (:player @worldwad))

  [#js [-0.001 -293.6045555555556] #js [0 383.6666666666667]]

  [#js [-0.001 -299.999] #js [0 0]]
  (let [{:keys [player walls]} @worldwad
        {:keys [x v]} player]
    (update-world @worldwad #{}))

  (defn group-by-transducer 
    ([key-fn]
     (fn [reduce-fn]
       (fn [acc x]
         (let [k (key-fn x)]
           (if (contains? acc k)
             (update acc k reduce-fn x)
             (assoc acc k x))))))

    ([key-fn dflt]
     (fn [reduce-fn]
       (fn [acc x]
         (let [k (key-fn x)]
           (if (contains? acc k)
             (update acc k reduce-fn x)
             (assoc acc k (reduce-fn dflt x))))))))

  (defn union-type [types]
    (as-> (distinct types) ts
      (if (> (count ts) 1)
        (cons '+ ts)
        (first ts))))

  (union-type '(a b a a a))

  (defn summarize-type [o]
    (cond
      (seq? o) (list 'Seq (union-type (map summarize-type o)))
      (vector? o) (list 'Vec (union-type (map summarize-type o)))
      (map? o) (list 'Map 
                     (union-type (map summarize-type (keys o)))
                     (union-type (map summarize-type (vals o))))
      (integer? o) 'integer
      (number? o) 'number
      (keyword? o) 'keyword
      true (type o)))
  (defn win? [b]
    (some (fn [x] (-> x set vec #{[:x] [:o]} first))
          (concat b (apply map list b) [(map nth b [0 1 2]) (map nth b [2 1 0])]))))
