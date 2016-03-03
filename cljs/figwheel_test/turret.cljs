(ns figwheel-test.turret
  (:require [figwheel-test.common
             :refer [tau canvas ctx fooprint init-elements scale-factor
                     with-viewport on-space]
             :as com]
            [figwheel-test.geometry :as g]
            [figwheel-test.canvas :as c])
  (:require-macros [figwheel-test.macros :as m]))

(defn screen->world [p]
  (let [[x y] (g/v- (g/vscale (/ 1 (scale-factor)) p) [640 480])]
    [x (- y)]))

(defn midpoint-displace [points mag iterations]
  (if (> iterations 0)
    (recur (concat
            (mapcat (fn [h1 h2]
                      [h1 (+ (/ (+ h1 h2) 2) (* (- (rand) 0.5) mag))])
                    points (rest points))
            [(last points)])
           (/ mag 2)
           (dec iterations))
    (vec points)))

(defn flatten-area [points y]
  (let [mid (/ (count points) 2)]
    (reduce
     (fn [points i] (assoc points i y))
     points
     (range (- mid 4) (+ mid 4)))))

(defn generate-terrain []
  (let [heights (midpoint-displace
                 [(- (rand-int 500) 250)
                  -215
                  (- (rand-int 500) 250)]
                 300 5)
        c (dec (count heights))]
    (-> heights
        (flatten-area -215)
        (->> (map-indexed
              (fn [ix p]
                [(- (/ (* 1280 ix) c) 640) p])))
        vec)))

(defn linterp [[x1 y1] [x2 y2] x]
  (+ y1 (/ (* (- x x1) (- y2 y1))
           (- x2 x1))))

(defn terrain-intersect
  "See if we've ended up under the terrain"
  [[x y] terrain]
  (let [dx (/ 1280 (dec (count terrain)))
        ix (quot (+ 640 x) dx)]
    (if (and (>= ix 0) (< ix (dec (count terrain))))
      (< y (linterp (terrain ix) (terrain (inc ix)) x))
      false)))

(defn maybe-dec [n]
  (if (= n 0) n (dec n)))

(defn alive? [state]
  (> (:health (:player state)) 0))

(def gen-id
  (let [n (atom 0)]
    (fn [] (swap! n (fn [x] (mod (+ x 1) 100000))))))

(defn spawn-bullet [state th pos]
  (let [dir (g/unit-vector th)]
    (update state :bullets assoc
            (gen-id)
            {:x (g/v+ pos (g/vscale 50 dir))
             :v (g/vscale 15 dir)})))

(defn spawn-enemy [state pos v]
  (update state :enemies assoc
          (gen-id)
          {:x pos :v v :w 0 :th 0
           :health 3}))

(defn turret-update [state path pointer trigger]
  (let [{:keys [th w cooldown k b pos temperature barrel-change ammo health]
         :as self} (get-in state path)
        dir (g/v- pointer pos)
        er [(js/Math.cos th) (js/Math.sin th)]
        delta (js/Math.atan2
               (g/vcross er dir)
               (g/vdot er dir))
        new-w (+ w (* k delta) (* -1 w b))
        new-th (+ th w)
        firing (and (= cooldown 0) (= barrel-change 0) (> ammo 0) trigger
                    (> health 0))
        new-temp (if firing (+ temperature 3) (* 0.995 temperature))]
    (-> (if firing (spawn-bullet state th pos) state)
        (assoc-in path
                  (-> self
                      (assoc :cooldown (if firing 5 (max (dec cooldown) 0)))
                      (assoc :temperature new-temp)
                      (assoc :barrel-change (if (> new-temp 100)
                                              600 (max (dec barrel-change) 0)))
                      (assoc :ammo (if firing (dec ammo) ammo))
                      (assoc :w (if (or (< new-th 0) (> new-th (/ tau 2)))
                                  0 new-w))
                      (assoc :th (com/clamp 0 new-th (/ tau 2))))))))

(defn turret-draw [{:keys [th cooldown pos health]} ctx]
  (c/with-saved-context
    ctx
    (fn []
      (let [[x y] pos]
        (.translate ctx x y)
        (if (> health 0)
          (do          
            (c/with-saved-context
              ctx
              (fn []
                (.rotate ctx th)
                (c/stroke-lines ctx [20 -3] [20 3] [40 3] [40 -3] [20 -3])
                (when (= cooldown 5)
                  (c/stroke-circle ctx [50 0] 10))))

            (set! (.-fillStyle ctx) "#fff")
            (.beginPath ctx)
            (.arc ctx 0 0 30 (/ tau -12) (/ (* 7 tau) 12))
            (.lineTo ctx (* 15 (js/Math.sqrt 3)) -15)
            (.fill ctx)
            (.stroke ctx))
          
          (c/stroke-lines
           ctx [-24 -15] [-28 -3] [-21 -4] [-14 5] [4 -4]
           [11 3] [25 0] [30 4] [28 -15] [-24 -15]))))))

(defn ray-circle-intersect [p d c r]
  (let [dc (g/v- p c)
        a (g/vdot d d)
        b (* (g/vdot dc d) 2)
        c (- (g/vdot dc dc) (* r r))
        descr (- (* b b) (* 4 a c))]
    (if (> descr 0)
      (/ (- (- b) (js/Math.sqrt descr)) (* 2 a))
      nil)))

(defn check-bullet-hits [bullet targets]
  (reduce (fn [a [name target]]
            (let [t (ray-circle-intersect
                     (:x bullet) (g/v- (:v bullet) (:v target))
                     (:x target) 10)]
              (if (and t (< 0 t 1))
                (if (or (not a) (< t (second a)))
                  (array name t)
                  a)
                a)))
          nil
          targets))

(defn particle-update
  "Common update function for all point-particle ballistic motions"
  [state type name {:keys [x v] :as self}]
  (if (let [pos x [x y] pos]
        (or (> (js/Math.abs x) 640)
            (> (js/Math.abs y) 480)
            (terrain-intersect (g/v+ pos v) (:terrain state))))
    (update state type dissoc name)
    (assoc-in state [type name]
              (-> self
                  (update :v g/v+ [0 -0.1])
                  (update :x g/v+ v)))))

(defn bullet-draw [{:keys [x v]} ctx]
  (c/stroke-lines ctx x (g/v- x v)))

(def chunk-types
  [(fn [{:keys [x]} ctx]
     (c/with-saved-context ctx
       (fn []
         (.translate ctx (first x) (second x))
         (.fillRect ctx -2.5 -2.5 5 5))))
   bullet-draw
   (fn [{:keys [x]} ctx]
     (c/stroke-circle ctx x 2))])

(defn splatter [state pos n min-speed max-speed]
  (reduce
   (fn [state junk]
     (let [mag (+ (* (rand) (- max-speed min-speed)) min-speed)
           angle (- (* (rand) (/ (* 2 tau) 3))
                    (/ tau 12))]
       (update state :chunks conj
               [(gen-id)
                {:x pos :v #js[(* mag (js/Math.cos angle))
                             (* mag (js/Math.sin angle))]
                 :draw (rand-nth chunk-types)}])))
   state
   (range n)))

(defn bullet-update [state name {:keys [x v] :as self}]
  (if-let [[enemy-id t] (check-bullet-hits self (:enemies state))]
    (-> state
        (update-in [:enemies enemy-id :v]
                   (fn [v2] (g/vscale (/ 1 1.25) (g/v+ v2 (g/vscale 0.25 v)))))
        (update-in [:enemies enemy-id :health] dec)
        (update :bullets dissoc name)
        (update :score inc)
        (splatter x 4 4 8))
    (particle-update state :bullets name self)))

(defn chunk-update [state name self]
  (particle-update state :chunks name self))

(defn circle-segment-toi
  "Find the lower bound of TOI for a circular object of radius r,
starting at point x, moving in velocity v, with a line
segment going from p1 to p2.  Returns nil if no impact."
  [x v r p1 p2]
  (let [dp (g/v- p2 p1)
        s (-> (g/vdot (g/v- p1 x) dp) (/ (g/vdot dp dp)) (* -1))
        s (max 0 (min 1 s))
        p (g/v+ p1 (g/vscale s dp))
        p-x (g/v- p x)
        |p-x| (g/vmag p-x)
        dist (- |p-x| r)
        vn (/ (g/vdot v p-x) |p-x|)]
    (if (< (js/Math.abs vn) 0.001)
      nil
      (/ dist vn))))

(defn ca-circle-segment [x0 v r p1 p2]
  (loop [t 0
         x x0
         n 0]
    (let [dt (circle-segment-toi x v r p1 p2)]
      (cond
        (nil? dt) nil
        (< (js/Math.abs dt) 0.01) t
        (< dt 0) nil
        (> (+ t dt) 1) nil
        true (recur (+ t dt) (g/v+ x (g/vscale dt v)) (inc n))))))

(defn enemy-terrain-check [{:keys [x v]} terrain]
  (let [dx (/ 1280 (dec (count terrain)))
        [x y :as pos] x
        vx (first v)
        ix1 (quot (+ 640 x (if (> vx 0) -10 10)) dx)
        ix2 (quot (+ 640 x (first v) (if (> vx 0) 10 -10)) dx)
        a (min ix1 ix2) b (max ix1 ix2)
        indices (range (max a 0) (min (inc b) (dec (count terrain))))]
    (reduce
     (fn [[a toi1 :as stuff] b]
       (let [p1 (terrain b)
             p2 (terrain (inc b))
             toi2 (ca-circle-segment pos v 10 p1 p2)]
         (if (and toi2 (or (nil? stuff) (< toi2 toi1)))
           [b toi2]
           stuff)))
     nil indices)))

(defn enemy-death [state name {:keys [x]}]
  (-> state
      (update :enemies dissoc name)
      (update :score + 5)
      (splatter x 15 6 12)))

(defn enemy-update [state name {:keys [x v w health] :as self}]
  (cond
    (let [pos x [x y] pos]
      (or (> (js/Math.abs x) 640)
          (> (js/Math.abs y) 480)))
    (update state :enemies dissoc name)

    (< health -3)
    (enemy-death state name self)

    (and (alive? state)
         (< (let [d (g/v- x (:pos (:player state)))] (g/vdot d d))
            1600))
    (-> state
        (enemy-death name self)
        (update-in [:player :health] dec))

    true
    (if-let [[ix toi] (enemy-terrain-check self (:terrain state))]
      (let [terrain (:terrain state)
            [dx dy] (g/v- (terrain (inc ix)) (terrain ix))
            norm (g/normalized [(- dy) dx])
            nv (g/vdot norm v)]
        (if (<= health 0)
          (enemy-death state name self)
          (assoc-in state [:enemies name]
                    (-> self
                        (update :x g/v+ (g/v+ (g/vscale toi v)
                                              (g/vscale 0.1 norm)))
                        (update :v g/v+ (g/vscale
                                         (+ (- nv) (max (* -0.5 nv)
                                                        (+ 2 (* (rand) 3))))
                                         norm))))))
      
      (assoc-in state [:enemies name]
                (-> self
                    (update :th + w)
                    (update :v g/v+ [0 -0.1])
                    (update :x g/v+ v))))))

(defn enemy-draw [{:keys [x th]} ctx]
  (c/with-saved-context ctx
    (fn []
      (.translate ctx (first x) (second x))
      (.rotate ctx th)
      (set! (.-fillStyle ctx) "#ffffff")
      (.beginPath ctx)
      (.arc ctx 0 0 10 0 tau)
      (.fill ctx)
      (.stroke ctx)
      (c/stroke-circle ctx #js[-3 2] 2)
      (c/stroke-circle ctx #js[3 2] 2)
      (c/stroke-lines ctx #js[-3 -5] #js[3 -5])
      (c/stroke-lines ctx #js[0 10] #js[0 13])
      (c/stroke-circle ctx #js[0 15] 2))))

(defn init-state []
  {:bullets {}
   :terrain (generate-terrain)
   :enemies {}
   :chunks {}
   :score 0
   :enemy-spawn 60
   :player {:th 0 :w 0 :cooldown 0 :k 0.02 :b 0.15
            :barrel-change 0
            :temperature 0
            :ammo 500
            :health 3
            :pos [0 -200]}})

(def stop (atom false))
(def dir (atom [1 0]))
(def state (atom (init-state)))
(def trigger (atom false))
(def high-score (atom 0))

(defn update-all [state key update-fn]
  (reduce-kv update-fn state (key state)))

(defn enemy-spawn-update [{:keys [enemy-spawn] :as state}]
  (if (alive? state)
    (if (= enemy-spawn 0)
      (let [x #js[(- (* 640 (rand)) 320) 480]
            v #js[(* (if (< (first x) 0) 4 -4) (rand))
                  (* (rand) -2)]]
        (-> state
            (assoc :enemy-spawn (+ (rand-int 300) 60))
            (spawn-enemy x v)))
      (update state :enemy-spawn dec))
    state))

(defn update-state [state]
  (let [new-state (-> state
                      (turret-update [:player] @dir @trigger)
                      (update-all :bullets bullet-update)
                      (update-all :enemies enemy-update)
                      (update-all :chunks chunk-update)
                      enemy-spawn-update)]
    (if (and (not (alive? new-state)) (alive? state))
      (do
        (splatter new-state (get-in new-state [:player :pos]) 20
                  10 20))
      new-state)))

(defn draw-state [state]
  (with-viewport
    #(do (when (not (alive? state))
           (com/center-print "Press Space to Continue"))
         (turret-draw (:player state) ctx)
         (run! (fn [[n x]] (bullet-draw x ctx))
               (:bullets state))
         (run! (fn [[n x]] (enemy-draw x ctx)) (:enemies state))
         (run! (fn [[n {:keys [draw] :as self}]] (draw self ctx))
               (:chunks state))
         (apply c/stroke-lines ctx (:terrain state))
         (.fillRect ctx -630 370 10 (:temperature (:player state)))
         (.strokeRect ctx -630 370 10 100)
         (.fillRect ctx -610 370 10 (/ (:barrel-change (:player state)) 6))
         (set! (.-font ctx) (str (/ 10 (scale-factor)) "px serif"))
         (.translate ctx -630 350)
         (.scale ctx 1 -1)
         (.fillText ctx (str "Ammo: " (:ammo (:player state))) 0 0)
         (.translate ctx 0 (/ 12 (scale-factor)))
         (.fillText ctx (str "Score: " (:score state)) 0 0))
    true))

(defn run-loop []
  (reset! stop false)
  
  (set! js/window.onmousemove
        (fn [evt]
          (reset! dir (-> [(.-pageX evt) (.-pageY evt)]
                          (g/v- (c/elem-offset canvas))
                          screen->world))))
  
  (on-space
   (fn []
     (if (alive? @state)
       (do (reset! stop true)
           (set! js/window.onmousemove nil)
           (on-space run-loop))
       (do (swap! high-score max (:score @state))
           (fooprint "High Score: " @high-score)
           (reset! state (init-state))))))

  (set! (.-onmousedown canvas) (fn [] (reset! trigger true) false))
  (set! (.-onmouseup js/window) (fn [] (reset! trigger false) false))
  
  (m/nlet lp []
          (when (not @stop)
            (draw-state @state)
            (swap! state update-state)
            (js/window.requestAnimationFrame lp))))

(defn init-everything []
  (init-elements)
  (draw-state @state)
  (com/center-print "Aim with the mouse, click to fire\nPress Space To Begin")
  (on-space run-loop))

(comment
  (m/cps-let [x (read-point* ctx (comp ! screen->world))
              x2 (do
                   (with-viewport (fn [] (c/stroke-circle ctx x 10)))
                   (read-point* ctx (comp ! screen->world)))]
             
             (let [[ix toi :as impact] (enemy-update
                                        {:x x :v (g/v- x2 x)}
                                        (:terrain @state))]
               (with-viewport
                 (fn []
                   (c/stroke-circle ctx
                                    (if impact
                                      (g/v+ x (g/vscale toi (g/v- x2 x)))
                                      x2)
                                    10)))

               (fooprint (pr-str [x x2 impact]))))

  (enemy-terrain-check
   {:x [504 81]
    :v (g/v- [588 52] [504 81])}
   (:terrain @state))

  (run-loop)

  (swap! state update :enemies conj [(name-gen) {:x [-640 200] :v [4 2]}])
  (swap! state update-in [:player :ammo] + 500)

  (check-bullet-hits (first (:bullets @state)) (:bullets @state))
  (:bullets @state)

  (init-everything)
  (init-elements)

  (run-loop)
  (swap! state spawn-enemy [-200 470] [2 0])

  (let [[name e] (first (:enemies @state))]
    (swap! state enemy-death name e)))
