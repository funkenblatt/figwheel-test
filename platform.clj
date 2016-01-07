(def world {:player {:x [0 0.001]
                     :v [0 0]}
            :walls [[[-640 -300] [0 -300]]
                    [[0 -300] [0 0]]
                    [[0 0] [640 0]]
                    [[-200 -250] [-100 -250]]]})

(defn draw-world [{:as world
                   :keys [walls player]}]
  (with-viewport
    (fn []
      (run! (fn [points] (apply c/stroke-lines com/ctx points)) walls)
      (c/stroke-circle com/ctx (g/v+ (:x player) [0 10]) 10))
    true))

(defn resting-on [x [w1 w2 :as wall]]
  (let [[wx wy] (g/v- w2 w1)
        norm (g/normalized [(- wy) wx])
        nd (g/vdot norm (g/v- x w1))]
    (and (<= (js/Math.abs nd) 0.002)
         (>= (* (g/vdot norm [0 1]) nd) 0))))

(def key-force-map {65 -800 68 800})

(def dt (/ 1 60))

(defn wall-hit? [p v [p1 p2 :as wall]]
  (let [s (g/ray-intersection p v wall)]
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
        (let [t (+ t s)
              dx (g/vscale (- dt (* dt t)) v)
              x (g/v+ x (g/vscale s dx))
              v (g/v- v (g/vscale (g/vdot norm v) norm))]
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
                          (- fx (min fx (* .01 vx vx)))
                          (+ fx (min (- fx) (* .01 vx vx))))
                        fx)))) +)
            0 keys)
    (* -4 vx)))

(defn update-world [{:as world
                     :keys [walls player]}
                    keys]
  (let [{player-x :x player-v :v} player
        grounded? (some (partial resting-on player-x) walls)
        gravity [0 -980]
        player-force (-> (if grounded?
                           [(foot-force keys player-v) 0]
                           [0 0])
                         (g/v+ gravity)
                         (g/v+ (g/vscale -0.1 player-v)))

        new-v (g/v+ player-v (g/v+ (g/vscale dt player-force)
                                   (if (and grounded? (keys 32))
                                     [0 400]
                                     [0 0])))
        [new-x new-v] (do-wall-collisions player-x new-v walls)]
    (-> (assoc-in world [:player :v] new-v)
        (assoc-in [:player :x] new-x))))

(def down-keys (atom #{}))

(comment
  (set! js/window.onkeydown (fn [e] (swap! down-keys conj (.-which e))))
  (set! js/window.onkeyup (fn [e] (swap! down-keys disj (.-which e))))


  (set! stop true)z

  (def worldwad (atom world))
  (reset! worldwad world)
  (do
    (set! stop false)
    (m/nlet lp []
      (when (not stop)
        (draw-world @worldwad)
        (swap! worldwad update-world @down-keys)
        (js/window.requestAnimationFrame lp))))

  (let [{:keys [player walls]} @worldwad
        {:keys [x v]} player]
    (update-world @worldwad #{})))
