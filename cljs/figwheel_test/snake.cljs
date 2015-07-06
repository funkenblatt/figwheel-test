(ns figwheel-test.snake
  (:require [figwheel-test.geometry :as g]
            [figwheel-test.canvas :as c]
            [clojure.core.rrb-vector :as rrb]))

;;; Snake Data
;;; [segment* ...]

;;; Segment
;;; {:type type :length length ...}

(def turning-radius 10)

(defn draw-snake [ctx {:keys [segments]}]
  (doseq [{type :type :as segment} segments]
    (case type
      :line (c/stroke-lines ctx (:p1 segment) (:p2 segment))
      :arc (let [{:keys [c th1 th2]} segment]
             (c/stroke-arc ctx c turning-radius th1 th2)))))

(defn update-head [{type :type :as segment} velocity]
  (update
   (case type
     :line (let [{:keys [p1 p2]} segment
                 dp (g/v- p2 p1)
                 mag (g/vmag dp)
                 dx (g/vscale (/ velocity mag) dp)]
             (update segment :p2 g/v+ dx))
     :arc (let [{:keys [th1 th2]} segment
                dir (.sign js/Math (- th2 th1))
                delta (* dir (/ velocity turning-radius))]
            (update segment :th2 + delta)))
   :length + velocity))

(comment
  (update-head {:type :arc
                :th1 0
                :th2 (/ js/Math.PI 2)
                :c [0 0]
                :length (* turning-radius (/ js/Math.PI 2))}
               .1))

(defn update-tail [{type :type :as segment} velocity]
  (update
   (case type
     :line (let [{:keys [p1 p2]} segment
                 dp (g/v- p2 p1)
                 mag (g/vmag dp)
                 dx (g/vscale (/ velocity mag) dp)]
             (update segment :p1 g/v+ dx))
     :arc (let [{:keys [th1 th2]} segment
                dir (js/Math.sign (- th2 th1))
                delta (* dir (/ velocity turning-radius))]
            (update segment :th1 + delta)))
   :length - velocity))

(comment
  (update-tail
   {:type :line
    :p1 [0 0]
    :p2 [1 0]
    :length 1}
   0.5))

(defn drop-segments [segments len-delta]
  (loop [i 0 len-delta len-delta]
    (if (< len-delta (:length (segments i)))
      (rrb/subvec (update segments i update-tail len-delta) i)
      (recur (+ i 1) (- len-delta (:length (segments i)))))))

(comment (drop-segments
          [{:length 1
            :type :line
            :p1 [0 0]
            :p2 [1 0]}
           {:length 4
            :type :line
            :p1 [1 0]
            :p2 [1 4]}
           {:length 2
            :type :line
            :p1 [1 4]
            :p2 [3 4]}]
          5.5))

(defn move-snake [{segments :segments
                   length :length} velocity total-length]
  (let [head-ix (dec (count segments))
        tail-v (- (+ length velocity) total-length)
        segments (update segments head-ix update-head velocity)]
    {:segments (if (> tail-v 0)
                 (drop-segments segments tail-v)
                 segments)
     :length (js/Math.min (+ length velocity) total-length)}))

(comment
  (def snakewad (atom {:segments [{:length 50
                                   :type :line
                                   :p1 [0 0]
                                   :p2 [50 0]}

                                  {:type :arc
                                   :c [50 10]
                                   :th1 (- (/ js/Math.PI 2))
                                   :th2 0
                                   :length (* (/ js/Math.PI 2) turning-radius)}

                                  {:length 50
                                   :type :line
                                   :p1 [60 10]
                                   :p2 [60 60]}]
                       :length (+ 100 (* (/ js/Math.PI 2) turning-radius))}))

  (let [ctx figwheel-test.core/ctx]
    (swap! snakewad move-snake 3 150)
    (c/with-saved-context ctx
      (fn []
        (c/clear ctx)
        (.translate ctx 640 480)
        (.scale ctx 1 -1)
        (draw-snake ctx @snakewad))))
  @snakewad)
