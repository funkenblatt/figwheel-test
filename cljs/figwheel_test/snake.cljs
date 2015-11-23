(ns figwheel-test.snake
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

(def turning-radius 25)

(defmulti draw-segment (fn [ctx segment] (:type segment)))

(defmethod draw-segment ::line [ctx {:keys [p1 p2]}]
  (c/stroke-lines ctx p1 p2))

(defmethod draw-segment ::arc [ctx {:keys [c th1 th2 dir r]}]
  (if (< dir 0)
    (c/stroke-arc ctx c r th2 th1)
    (c/stroke-arc ctx c r th1 th2)))

(defmulti update-seg
  "Update some part of a segment"
  (fn [segment velocity end] (:type segment)))

(defmethod update-seg ::line [{:keys [dir] :as segment} velocity end]
  (let [dx (g/vscale velocity dir)]
    (update segment (if (= end :head) :p2 :p1) g/v+ dx)))

(defmethod update-seg ::arc [{:keys [dir r] :as segment} velocity end]
  (let [delta (* dir (/ velocity r))]
    (update segment (if (= end :head) :th2 :th1) + delta)))

(defn update-head [segment velocity]
  (update
   (update-seg segment velocity :head)
   :length + velocity))

(defn update-tail [{type :type :as segment} velocity]
  (update
   (update-seg segment velocity :tail)
   :length - velocity))

(defn drop-segments [segments len-delta]
  (loop [i 0 len-delta len-delta]
    (when (= i (count segments))
      (js/alert "Uh... somehow our snake disappeared.")
      (throw (js/Error. "WTF")))
    (if (< len-delta (:length (segments i)))
      (rrb/subvec (update segments i update-tail len-delta) i)
      (recur (+ i 1) (- len-delta (:length (segments i)))))))

(defn move-snake [{segments :segments
                   length :length :as snake} velocity total-length]
  (let [head-ix (dec (count segments))
        tail-v (- (+ length velocity) total-length)
        segments (update segments head-ix update-head velocity)]
    (-> snake
        (assoc :segments (if (> tail-v 0)
                           (drop-segments segments tail-v)
                           segments)
               :length (js/Math.min (+ length velocity) total-length)))))

(defmulti seg-normal
  "Find the normal vector at the head of this segment for
  the given turn direction."
  (fn [segment dir] (:type segment)))

(defmethod seg-normal ::line [{:keys [dir]} turn]
  (let [[dx dy] dir]
    (if (= turn :left) (array (- dy) dx) (array dy (- dx)))))

(defmethod seg-normal ::arc [{:keys [th1 th2 dir]} turn]
  (let [multiplier (* dir (if (= turn :left) -1 1))]
    (g/vscale multiplier [(js/Math.cos th2) (js/Math.sin th2)])))

(defmulti seg-endpoint "Find the coordinates of this segment's end point" :type)
(defmethod seg-endpoint ::line [segment] (:p2 segment))
(defmethod seg-endpoint ::arc [{:keys [th1 th2 c r]}]
  (g/v+ c (array (* r (js/Math.cos th2))
                 (* r (js/Math.sin th2)))))

(defn add-segment [{:as snake :keys [segments]} segment]
  (assoc snake :segments
         (if (= (:length (last segments)) 0)
           (conj (pop segments) segment)
           (conj segments segment))))

(defn turn-snake
  "Possibly add new segments depending on whether the turn direction
as changed."
  [{:as snake :keys [turn segments]} new-turn]
  (if (not= turn new-turn)
    (-> snake
        (assoc :turn new-turn)
        (add-segment
         (case new-turn
           (:left :right)
           (let [head (last segments)
                 [normx normy :as norm] (seg-normal head new-turn)
                 c (g/v+ (seg-endpoint head) (g/vscale turning-radius norm))
                 th1 (js/Math.atan2 (- normy) (- normx))
                 th2 th1
                 dir (if (= new-turn :left) 1 -1)]
             {:type ::arc :c c :th1 th1 :th2 th2 :length 0 :dir dir
              :r turning-radius})

           ;; Otherwise, we're adding a straight section to a turn
           (let [{:keys [th2 dir] :as head} (last segments)
                 p1 (seg-endpoint head)]
             {:type ::line :p1 p1 :p2 p1 :length 0
              :dir (g/vscale dir (array (- (js/Math.sin th2))
                                        (js/Math.cos th2)))}))))
    snake))

(defn draw-shit [ctx game-state]
  (with-viewport
    (fn []
      (let [[x y] (seg-endpoint (last (:segments game-state)))]
        (.translate ctx
                    (- (max (min x (- 640 (/ (.-width canvas) 2)))
                            (- (/ (.-width canvas) 2) 640)))
                    (- (max (min y (- 480 (/ (.-height canvas) 2)))
                            (- (/ (.-height canvas) 2) 480))))
        (run! (partial draw-segment ctx)
              (concat (:walls game-state)
                      (map val (:targets game-state))
                      (:segments game-state)))))
    true))

(defn contains-angle? [{:keys [th1 th2 dir]} angle]
  (if (> (* dir (- th2 th1)) tau)
    true
    (let [angle (mod angle tau)
          th1 (mod th1 tau)
          th2 (mod th2 tau)]
      (if (< dir 0)
        (if (< th2 th1)
          (<= th2 angle th1)
          (or (<= 0 angle th1)
              (<= th2 angle tau)))
        (if (< th1 th2)
          (<= th1 angle th2)
          (or (<= 0 angle th2)
              (<= th1 angle tau)))))))

(defmulti check-intersection
  "See if the two given segments intersect at all"
  (fn [a b] [(:type a) (:type b)]))

(defmethod check-intersection [::line ::line] [a b]
  (let [[t1 t2 :as ts] (g/find-intersections
                        [(:p1 a) (:p2 a)] [(:p1 b) (:p2 b)])]
    (and ts (< 0 t1 1) (< 0 t2 1))))

(defmethod check-intersection [::line ::arc] [a b]
  (let [{:keys [p1 p2]} a
        {:keys [r c]} b
        [t1 t2 ph1 ph2 :as ts] (g/line-arc-intersect r c p1 p2)]
    (and ts (or
             (and (< 0 t1 1) (contains-angle? b ph1))
             (and (< 0 t2 1) (contains-angle? b ph2))))))

(defmethod check-intersection [::arc ::line] [a b]
  (check-intersection b a))

(defmethod check-intersection [::arc ::arc] [a b]
  (let [[a1 a2 b1 b2 :as angles] (g/arc-intersection
                                   (:c a) (:r a) (:c b) (:r b))]
    (and angles
         (or (contains-angle? a a1)
             (contains-angle? a a2))
         (or (contains-angle? b b1)
             (contains-angle? b b2)))))

(defn check-walls [{:keys [walls segments] :as game-state}]
  (let [head (last segments)
        segments (pop segments)
        intersects? #(check-intersection head %)]
    (some intersects? (concat walls (if (not-empty segments)
                                      (pop segments)
                                      segments)))))

(defn check-targets [{:keys [targets segments]
                      :as game-state}]
  (let [head (last segments)]
    (->> targets
         (filter (comp (partial check-intersection head) val))
         (reduce (fn [state [name _]]
                   (-> (update state :target-length + 50)
                       (update :targets dissoc name)))
                 game-state))))

(def death-state (atom nil))

(defn make-target [ctx {:keys [walls] :as game-state}]
  (let [can (.-canvas ctx)
        w (.-width can) h (.-height can)]
    (loop [x (- (rand-int w) (/ w 2)) y (- (rand-int h) (/ h 2))]
      (if (->> walls
               (map (comp #(g/closest-approach % [x y]) (juxt :p1 :p2)))
               (some #(< % 20)))
        (recur (- (rand-int w) (/ w 2))
               (- (rand-int h) (/ h 2)))
        [x y]))))

(def starting-segments
  [{:length 100
    :type ::line
    :dir [1 0]
    :p1 [0 0]
    :p2 [100 0]}])

(defn poly->segments [p]
  (map (fn [p1 p2] {:type ::line :p1 p1 :p2 p2}) p (rest p)))

(defn init-snake
  "Set the game to its initial state."
  ([game-state ctx]
   (init-snake game-state ctx
               (l/levels (mod (:level game-state) (count l/levels)))))
  ([game-state ctx level]
   (reduce
    (fn [state n] (update state :targets
                          assoc n {:type ::arc :c (make-target ctx state)
                                   :th1 0 :th2 tau :dir 1 :r 10}))
    (assoc game-state
           :segments starting-segments
           :length 100
           :target-length 100
           :turn nil
           :targets {}
           :walls (concat
                   (poly->segments [[-642 482] [642 482] [642 -482] [-642 -482] [-642 482]])
                   level)
           :stop false)
    (range 10))))

(def my-snake (atom {:level 0
                     :level-tries (sorted-map)}))

(declare run-shit)
(declare set-start!)

(defn set-pause! [ctx on-death on-win]
  (when (not mobile?)
    (on-space
     (fn []
       (swap! my-snake assoc :stop true)
       (js/window.requestAnimationFrame
        #(center-print "Paused"))
       (on-space
        (fn []
          (swap! my-snake assoc :stop false)
          (run-shit ctx on-death on-win)
          (set-pause! ctx on-death on-win)))))))

(def unset-keys #(do (set! js/window.onkeydown nil)
                     (set! js/window.onkeyup nil)))

(defn record-try []
  (swap! my-snake update-in
         [:level-tries (:level @my-snake)]
         (fnil inc 0)))

(defn show-level-tries [ctx level-tries y-start]
  (with-viewport
    (fn []
      (let [max-tries (->> level-tries (map val) (reduce max))
            font "16px serif"
            max-width (->> level-tries
                           (map (comp (partial com/text-width font)
                                      (partial str "Level ")
                                      val))
                           (reduce max))
            width (+ max-width 10 220 (com/text-width font (str max-tries)))
            offset (/ width -2)]
        (reduce
         (fn [y [level tries]]
           (com/viewport-print offset y (str "Level " level)
                               :font font)
           (.strokeRect ctx (+ offset max-width 10) (- y 4)
                        (/ (* 200 tries) max-tries) 17)
           (com/viewport-print (+ offset max-width 220) y (str tries) :font font)
           (- y 25))
         y-start level-tries)))
    false))

(defn levels-complete
  "Show the victory screen and then do other stuff."
  []
  (c/fade-out [255 255 255] 30 ctx
              (fn []
                (show-level-tries ctx (:level-tries @my-snake) 150)
                (com/viewport-print
                 0 180 "You did it, Snake!  Here's how many tries it took you to beat each level."
                 :align :center)

                (swap! my-snake assoc :level-tries (sorted-map) :level 0)
                (on-space #(do (set-start! ctx "Ready to try again?"))))))

(defn set-start!
  ([ctx] (set-start! ctx "Press space or tap near the center when ready"))
  ([ctx message]
   (swap! my-snake init-snake ctx)
   (draw-shit ctx @my-snake)
   (center-print (str "\n\n" message))
   (on-space
    (fn []
      (try (js/mixpanel.track
            "start level"
            #js{:level (:level @my-snake)})
           (catch js/Error e ))
      
      (let [on-death (fn [ctx]
                       (record-try)
                       (unset-keys)
                       (center-print
                        "\n\n\nSnake?  Snake?! SNAAAAAAAAKE!!")
                       (on-space #(set-start! ctx)))

            on-win (fn [ctx]
                     (record-try)
                     (swap! my-snake update :level inc)
                     (unset-keys)
                     (if (= (:level @my-snake) (count l/levels))
                       (levels-complete)
                       (do
                         (center-print
                          "\n\n\nYou did it, Snake!  Unfortunately there's another facility
we need you to infiltrate.")
                         (on-space #(set-start! ctx)))))]
        (run-shit ctx on-death on-win)
        (set-pause! ctx on-death on-win))))))

(def turn-map {65 :left 37 :left 68 :right 39 :right})

(defn touch->turn [evt]
  (let [touch-list (-> evt .-touches (array-seq 0))]    
    (when (> (count touch-list) 0)
      (let [[px py] (->> touch-list
                         last
                         (c/canvas-coord ctx)
                         undo-viewport)]
        (cond (< px -40) :left
              (> px 40) :right
              :else nil)))))

(defn run-shit [ctx on-death on-win]
  (set! js/window.onkeydown
                (fn [evt]
                  (if-let [turn (turn-map (.-which evt))]
                    (swap! my-snake turn-snake turn))))
  (set! js/window.onkeyup
        (fn [evt]
          (if-let [turn (turn-map (.-which evt))]
            (if (= turn (:turn @my-snake))
              (swap! my-snake turn-snake nil)))))
  (set! (.-ontouchstart canvas)
        (fn [evt]
          (.preventDefault evt)
          (let [{:as head :keys [dir p1]} (last (:segments @my-snake))]
            (swap! my-snake turn-snake (touch->turn evt)))))
  (set! (.-ontouchend canvas)
        (fn [evt]
          (.preventDefault evt)
          (swap! my-snake turn-snake (touch->turn evt))))
  
  ((fn loopage []
     (if (not (:stop @my-snake))
       (let [updated (swap! my-snake
                            (fn [state]
                              (check-targets
                               (move-snake state 3 (:target-length state)))))]
         (cond (check-walls updated) (on-death ctx)

               (empty? (:targets updated)) (on-win ctx)

               true
               (do (draw-shit ctx @my-snake)
                   (js/window.requestAnimationFrame loopage))))
       (unset-keys)))))

(defn ^:export init-everything []
  (init-elements)
  (set-start! ctx
              "

Snake!  We need you to infiltrate this 2D facility and retrieve
all of the plans for Plastic Gear!  Don't touch any of the walls in the facility
though, they're coated with a deadly neurotoxin!  Also, don't touch yourself either,
we've heard that's bad for you.

You'll need to turn left and right using the A and D keys.

Press space or tap near the center of the screen to start"))

(comment
  (update-head {:type ::arc
                :th1 (/ js/Math.PI 2)
                :th2 0
                :dir -1
                :r turning-radius
                :c [0 0]
                :length (* turning-radius (/ js/Math.PI 2))}
               2)
  (update-tail {:type ::line
                       :p1 [0 0]
                       :p2 [1 0]
                       :length 1}
               0.5)
  (drop-segments
   [{:length 1
     :type ::line
     :dir [1 0]
     :p1 [0 0]
     :p2 [1 0]}
    {:length 4
     :type ::line
     :dir [0 1]
     :p1 [1 0]
     :p2 [1 4]}
    {:length 2
     :type ::line
     :dir [1 0]
     :p1 [1 4]
     :p2 [3 4]}]
   0.5)

  (seg-normal {:type ::line :p1 [0 0] :p2 [4 0] :dir [1 0]} :left)
  (seg-normal {:type ::arc :th1 0 :th2 (/ js/Math.PI 2) :dir 1} :left)
  (seg-normal {:type ::arc :th1 (/ js/Math.PI 2) :th2 0 :dir -1} :left))
