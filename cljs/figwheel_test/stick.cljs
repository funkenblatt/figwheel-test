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
(def arm-length 12)
(def arm-anchor [0 -1.5])
(def leg-length 12)
(def head-radius 6)

(defn polar [r th]
  (g/vscale r (g/unit-vector th)))

(defn draw-figure [ctx {:keys [left-arm right-arm left-leg right-leg]}]
  (with-viewport
    (fn []
      (.scale com/ctx 4 4)
      (set! (.-lineJoin ctx) "round")
      (c/stroke-circle ctx (g/v+ neck [0 head-radius]) head-radius)
      (c/stroke-lines ctx neck groin)
      (run! (fn [[a r angles]]
              (apply c/stroke-lines-relative ctx
                     a (map (partial polar r) angles)))
            [[arm-anchor arm-length left-arm]
             [arm-anchor arm-length right-arm]
             [groin leg-length left-leg]
             [groin leg-length right-leg]]))
    true))

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

(defn interpolate [state1 state2]
  (fn [t]
    (reduce-kv
     (fn [s k v]
       (update s k (fn [v1] (g/v+ v1 (g/vscale t (g/v- v v1))))))
     state1
     state2)))

(defn evt-point [e]
  (g/vscale 0.25 (undo-viewport (c/canvas-coord com/ctx e))))

(def limb-state
  (atom {:left-arm [(/ tau -2) (/ tau -2)]
         :right-arm [0 0]
         :left-leg [(/ tau -3) (/ tau -3)]
         :right-leg [(/ tau -6) (/ tau -6)]}))

(defn end-position [anchor l angles]
  (reduce g/v+ anchor (map (partial polar l) angles)))

(def limb-anchor {:left-arm arm-anchor
                  :right-arm arm-anchor
                  :left-leg groin
                  :right-leg groin})

(def limb-length {:left-arm arm-length
                  :right-arm arm-length
                  :left-leg leg-length
                  :right-leg leg-length})

(defn closest-end [limb-state point]
  (reduce-kv
   (fn [a k v]
     (min-key (fn [x]
                (g/vmag
                 (g/v- point
                       (end-position (limb-anchor x)
                                     (limb-length x)
                                     (limb-state x)))))
              a k))
   (key (first limb-state))
   limb-state))

(defn ik-playground []
  (set! (.-onmousedown canvas)
        (fn [e]
          (let [p (evt-point e)
                limb (closest-end (:limbs @limb-state) p)]
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
                    (swap! limb-state assoc-in [:limbs limb]
                           (ik-angles (limb-anchor limb)
                                      (limb-length limb)
                                      (evt-point e)
                                      (get-in @limb-state [:dirs limb] -1)))
                    (js/requestAnimationFrame
                     #(draw-figure com/ctx (:limbs @limb-state)))))))))
