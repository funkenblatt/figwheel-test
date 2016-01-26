(ns figwheel-test.stick
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
            clojure.string)
  (:require-macros [figwheel-test.macros :as m]))

(def neck [0 3])
(def groin [0 -20])
(def arm-length 10)
(def leg-length 12)
(def head-radius 6)
(def torso-length 23)

(defn polar [r th]
  (g/vscale r (g/unit-vector th)))

(defn draw-figure [ctx {{:keys [torso]
                         :as limbs} :limbs
                         groin :groin
                         :as state}
                   & {:keys [offset dir]
                      :or {dir 1}}]
  (let [torso-angle (first (:angles torso))
        arm-anchor (g/v+ groin (polar 19.5 torso-angle))]
    (c/with-saved-context com/ctx
      (fn []
        (when offset (.translate com/ctx (first offset) (second offset)))
        (.scale com/ctx 2.5 2.5)
        (if (> dir 0) (.scale com/ctx -1 1))
        (set! (.-lineJoin ctx) "round")
        ;; Draw head
        (c/stroke-circle
         ctx (g/v+ groin (polar (+ 23 head-radius) torso-angle))
         head-radius)
        ;; Draw torso
        (c/stroke-lines-relative ctx groin (polar 23 torso-angle))
        ;; Draw limbs
        (run!
         (fn [[limb-name {:keys [angles length anchor]}]]
           (apply c/stroke-lines-relative ctx
                  (anchor state)
                  (map (partial polar length) angles)))
         limbs)))))

(defn ik-angles
  ([anchor l target]
   (ik-angles anchor l target 1))
  ([anchor l target dir]
   (let [dx (g/v- target anchor)
         th (g/vec-angle dx)
         r (g/vmag dx)
         dth (if (> r (* 2 l))
               0
               (* dir (js/Math.acos (/ r (* 2 l)))))]
     [(+ th dth) (- th dth)])))

(defn interpolate [state1 state2 t]
  (let [interp (fn [a b] (+ a (* t (- b a))))]
    (reduce-kv
     (fn [s k {v :angles}]
       (update-in s [:limbs k :angles] (fn [v1] (mapv interp v1 v))))
     (update state1 :groin (fn [v1] (mapv interp v1 (:groin state2))))
     (:limbs state2))))

(defn cps-run! [f s k]
  (if (not-empty s)
    (m/cps-let [junk (f (first s) !)]
               (cps-run! f (rest s) k))
    (k nil)))

(defn interpolations
  "Interpolate key frames."
  [frames n]
  (for [[f1 f2] (map vector frames (rest frames))
        :let [interpolator (partial interpolate f1 f2)]
        i (range n)]
    (interpolator (/ i n))))

(defn evt-point [e]
  (g/vscale (/ 1.0 2.5) (undo-viewport (c/canvas-coord com/ctx e))))

(defn arm-anchor [{:keys [groin limbs]}]
  (g/v+ groin (polar 19.5 (get-in limbs [:torso :angles 0]))))

(def limb-state
  (atom {:limbs {:left-arm {:angles [(/ tau -2) (/ tau -2)]
                            :length arm-length
                            :anchor arm-anchor}
                 :right-arm {:angles [0 0]
                             :length arm-length
                             :anchor arm-anchor}
                 :left-leg {:angles [(/ tau -3) (/ tau -3)]
                            :length leg-length
                            :anchor :groin}
                 :right-leg {:angles [(/ tau -6) (/ tau -6)]
                             :length leg-length
                             :anchor :groin}
                 :torso {:angles [(/ tau 4)]
                         :anchor :groin
                         :length torso-length}}
         :groin [0 -20]
         :dirs {}}))

(defn end-position [anchor l angles]
  (reduce g/v+ anchor (map (partial polar l) angles)))

(defn closest-end [{:keys [limbs] :as state} point]
  (reduce-kv
   (fn [a k v]
     (min-key (fn [x]
                (let [{:keys [anchor length angles]} (limbs x)]
                  (->> (end-position (anchor state) length angles)
                       (g/v- point)
                       g/vmag)))
              a k))
   (key (first limbs))
   limbs))

(def keyframes (atom []))

(defn ik-playground []
  (set! (.-onmousedown canvas)
        (fn [e]
          (let [p (evt-point e)
                limb (closest-end @limb-state p)
                {:keys [anchor length]} (get-in @limb-state [:limbs limb])]
            (js/console.log limb)
            (set! (.-onmouseup canvas)
                  (fn [e]
                    (set! (.-onmouseup canvas) nil)
                    (set! (.-onmousemove canvas) nil)
                    (set! (.-onwheel canvas) nil)))
            
            (set! (.-onwheel canvas)
                  (fn [e]
                    (swap! limb-state update-in [:dirs limb]
                           (fn [x] (- (or x -1))))
                    ((.-onmousemove canvas) e)))
            
            (set! (.-onmousemove canvas)
                  (fn [e]
                    (swap! limb-state
                           (fn [{:keys [limbs dirs] :as state}]
                             (let [p (evt-point e)]
                               (assoc-in
                                state
                                [:limbs limb :angles]
                                (if (= (count (get-in limbs [limb :angles])) 1)
                                  (let [[x y] (g/v- p (anchor state))]
                                    [(js/Math.atan2 y x)])
                                  (ik-angles (anchor state)
                                             length p
                                             (get dirs limb -1)))))))
                    (js/requestAnimationFrame
                     #(do
                        (with-viewport
                          (fn []
                            (draw-figure com/ctx @limb-state :dir -1))
                          true)))))))))


(def startwad (assoc-in @limb-state [:limbs :torso :angles] [1.7209188128106587]))
(def walk-ik-data
  [[#js[-12 -20.785] #js[12 -20.785] [11 -15] [-11 -15]]
   [#js[0 -20.785] #js[0 -15.785] [0 -18.6] [0 -18.6]]
   [#js[12 -20.785] #js[-12 -20.785] [-11 -15] [11 -15]]
   [#js[0 -15.785] #js[0 -20.785] [0 -18.6] [0 -18.6]]])

(def walk-frames
  (mapv
   (fn [[left right left-arm right-arm]]
     (-> startwad
         (assoc-in [:limbs :left-leg :angles] (ik-angles [0 0] leg-length left -1))
         (assoc-in [:limbs :right-leg :angles] (ik-angles [0 0] leg-length right -1))
         (assoc-in [:limbs :left-arm :angles] (ik-angles [0 0] arm-length left-arm))
         (assoc-in [:limbs :right-arm :angles] (ik-angles [0 0] arm-length right-arm))))
   walk-ik-data))
(def jump-walk-frames
  (mapv
   (comp
    (fn [[left right left-arm right-arm]]
      (-> (update startwad :groin g/v+ [0 -5])
          (assoc-in [:limbs :left-leg :angles] (ik-angles [0 0] leg-length left -1))
          (assoc-in [:limbs :right-leg :angles] (ik-angles [0 0] leg-length right -1))
          (assoc-in [:limbs :left-arm :angles] (ik-angles [0 0] arm-length left-arm))
          (assoc-in [:limbs :right-arm :angles] (ik-angles [0 0] arm-length right-arm))))
    (fn [stuff]
      (map (partial g/v+ [0 5]) stuff)))
   walk-ik-data))

(def walk-cycle (vec
                 (interpolations (concat walk-frames [(first walk-frames)]) 12)))
(def crouch-cycle (vec
                   (interpolations (concat jump-walk-frames [(first jump-walk-frames)]) 12)))

(defn update-walk [{:keys [limb-state phase x crouched dir]
                      :as state}
                     down-keys]
    (let [new-state (cond
                      (and (down-keys 17) (< crouched 5)) (update state :crouched inc)
                      (and (not (down-keys 17)) (> crouched 0)) (update state :crouched dec)
                      true state)
          new-state (if (or (down-keys 65) (down-keys 68))
                      (update new-state :phase com/modinc 48)
                      (if (not (= (mod phase 24) 0))
                        (update new-state :phase com/modinc 48)
                        new-state))
          new-state (if (not (identical? state new-state))
                      (let [{:keys [phase crouched]} new-state]
                        (assoc new-state :limb-state (interpolate
                                                      (walk-cycle phase)
                                                      (crouch-cycle phase)
                                                      (/ crouched 5))))
                      state)
          new-state (if (and
                         (not (every? down-keys #{65 68}))
                         (or (and (> dir 0) (down-keys 65))
                             (and (< dir 0) (down-keys 68))))
                      (update new-state :dir * -1)
                      new-state)]
      new-state))

(comment

  (def stop false)
  ;; Janky walking animation

  (let [horizontal (atom 150)
        velocity 1]
    (cps-run!
     (fn [x k]
       (c/clear com/ctx)
       (draw-figure com/ctx x :offset [@horizontal 0] :dir -1)
       (swap! horizontal - velocity)
       (when (not stop)
         (js/requestAnimationFrame #(k nil))))
     ;; (interpolations
     ;;  [(first walk-frames)
     ;;   (first jump-walk-frames)]
     ;;  30)
     (take 240 (interpolations
                (cycle (concat walk-frames walk-frames jump-walk-frames))
                12))
     identity))

  {:limbs 
   {:left-arm {:angles [2.727112540161899 2.727112540161899], :length 10}
    :right-arm {:angles [-0.2950464590881281 1.065894037376769], :length 10}
    :left-leg {:angles [-2.02186598278342 -2.02186598278342], :length 12, :anchor :groin}
    :right-leg {:angles [-0.9652516631899266 -0.9652516631899266], :length 12, :anchor :groin}
    :torso {:angles [1.2583047670603775], :anchor :groin, :length 23}}
   :groin [0 -20], :dirs {:left-arm 1}}

  (m/nlet lp []
    (when (not stop)
      (swap! statewad update-walk @down-keys)
      (c/clear com/ctx)
      (draw-figure com/ctx (:limb-state @statewad) :dir (:dir @statewad))
      (js/window.requestAnimationFrame lp)))

  (set! stop true)
  
  (c/clear com/ctx)
  (draw-figure com/ctx (:limb-state @statewad))

  )
