(ns figwheel-test.editor
  (:require-macros [figwheel-test.macros :as m])
  (:require [figwheel-test.canvas :as c]
            [figwheel-test.geometry :as g]
            [figwheel-test.snake :as s]))

(defn canvas-coord [ctx evt]
  (let [can (.-canvas ctx)
        [x y] (c/elem-offset can)]
    (array (- (.-pageX evt) x)
           (- (.-pageY evt) y))))

(defn read-point [ctx cont]
  (let [can (.-canvas ctx)
        finish (fn [val]
                 (set! (.-onmouseup can) nil)
                 (set! (.-onkeypress js/window) nil)
                 (set! (.-oncontextmenu js/window) nil)
                 (cont val))]
    (set! (.-oncontextmenu js/window) (fn [] false))
    (set! (.-onmouseup can)
          (fn [e]
            (if (= (.-button e) 0)
              (finish (canvas-coord ctx e))
              (finish nil))))
    (set! (.-onkeypress js/window) (fn [] (finish nil)))))

(defn complete-line-draw [ctx cont draw p1]
  (set! (.-onmousemove (.-canvas ctx))
        (fn [e]
          (js/window.requestAnimationFrame
           (fn []
             (c/clear ctx)
             (draw ctx)
             (c/stroke-lines ctx p1 (canvas-coord ctx e))))))
  (m/cps-let [p2 (read-point ctx !)]
             (set! (.-onmousemove (.-canvas ctx)) nil)
             (cont p2)))

(defn read-polyline-draw [ctx cont draw]
  (m/cps-let [start (read-point ctx !)]
    (if start
      (m/nlet lp [points [start]]
        (let [draw* (fn [ctx]
                      (draw ctx)
                      (when (> (count points) 1)
                        (apply c/stroke-lines ctx points)))]
          (m/cps-let [p (complete-line-draw ctx ! draw* (last points))]
            (if p
              (lp (conj points p))
              (cont points)))))
      (cont nil))))

(def state (atom {:walls {}
                  :selected nil
                  :id 0}))

(defn draw-state [{:keys [selected walls] :as state} ctx]
  (c/stroke-circle ctx [640 480] 2)
  (c/stroke-circle ctx [740 480] 2)
  (run! (fn [[key poly]]
          (c/with-saved-context
            ctx
            (fn []
              (when (and selected (= selected key))
                (set! (.-strokeStyle ctx) "#ff0000"))
              (apply c/stroke-lines ctx poly))))
        walls))

(defn clear-handlers [ctx]
  (let [can (.-canvas ctx)]
    (c/clear ctx)
    (draw-state @state ctx)
    (set! (.-onmousemove can) nil)
    (set! (.-onmouseup can) nil)
    (set! (.-onkeypress js/window) nil)
    (set! (.-oncontextmenu js/window) nil)))

(defn add-walls [ctx]
  (clear-handlers ctx)
  (read-polyline-draw
   ctx
   (fn [poly]
     (when (> (count poly) 1)
       (swap! state (fn [{:keys [walls id]
                          :as state}]
                      (-> state
                          (assoc-in [:walls id] poly)
                          (update :id inc)))))
     (js/window.requestAnimationFrame
      (fn []
        (c/clear ctx)
        (draw-state @state ctx)
        (add-walls ctx))))
   (fn [ctx] (draw-state @state ctx))))

(defn undo-viewport [[x y]]
  [(- x 640) (+ 480 (- y))])

(defn to-level [{:as state :keys [walls]}]
  (into [] (for [[k poly] walls
                 [p1 p2] (map vector poly (rest poly))]
             {:type :figwheel-test.snake/line
              :p1 (undo-viewport p1) :p2 (undo-viewport p2)})))

(defn nearest-thing [{:as state :keys [walls]} p]
  (reduce (partial min-key second)
          (for [[k poly] walls
                segment (map vector poly (rest poly))]
            [k (g/closest-approach segment p)])))

(defn select-thing [ctx]
  (clear-handlers ctx)
  (set! (-> ctx .-canvas .-onmouseup)
        (fn [e]
          (let [p (canvas-coord ctx e)]
            (let [[key distance] (nearest-thing @state p)]
              (swap! state assoc :selected
                     (if (< distance 20) key nil))
              (c/clear ctx)
              (draw-state @state ctx)))))

  (set! (-> js/window .-onkeypress)
        (fn [e]
          (when (and (= (.-which e) 8)
                     (:selected @state))
            (swap! state (fn [{:keys [selected] :as s}]
                           (update s :walls dissoc selected)))
            (c/clear ctx)
            (draw-state @state ctx)))))

