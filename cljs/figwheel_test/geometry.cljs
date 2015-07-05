(ns figwheel-test.geometry)

(defn v- [[ax ay] [bx by]] (array (- ax bx) (- ay by)))
(defn v+ [[ax ay] [bx by]] (array (+ ax bx) (+ ay by)))
(defn vscale [s [ax ay]] (array (* s ax) (* s ay)))

(defn vdot [[ax ay] [bx by]]
  (+ (* ax bx) (* ay by)))

(defn vsquare [v] (vdot v v))

(defn vmag [[x y]] (.sqrt js/Math (+ (* x x) (* y y))))

(defn v= [[ax ay] [bx by]] (and (= ax bx) (= ay by)))

(defn edges [poly]
  (map array poly (rest poly)))

(defn offset-edges [poly distance]
  (for [[p1 p2] (edges poly)]
    (let [[dx dy] (v- p2 p1)
          normal [(- dy) dx]
          normal (vscale (/ distance (vmag normal)) normal)]
      [(v+ p1 normal) (v+ p2 normal)])))

(defn find-intersections
  "Determine where two line segments intersect."
  [[a1 a2] [b1 b2]]
  (let [[dax day] (v- a2 a1)
        [dbx dby] (v- b2 b1)
        [abx aby] (v- b1 a1)
        det (- (* -1 dax dby) (* -1 day dbx))
        s (/ (+ (* dby abx -1) (* dbx aby)) det)
        t (/ (+ (* -1 day abx) (* dax aby)) det)]
    [s t]))

(defn trimmed [segments]
  (let [segs (concat segments [(first segments)])
        out
        (for [[s1 [p1 p2]] (map array segs (rest segs))]
          (let [i (first (find-intersections [p1 p2] s1))
                dx (v- p2 p1)]
            (v+ p1 (vscale i dx))))]
    (concat out [(first out)])))

(defn ray-intersection [origin [dax day] [p1 p2]]
  (let [[dbx dby] (v- p2 p1)
        [abx aby] (v- p1 origin)
        det (- (* day dbx) (* dax dby))
        s (/ (- (* dbx aby) (* dby abx)) det)
        t (/ (- (* dax aby) (* day abx)) det)]
    (if (<= 0 t 1) s -1)))

(defn offset-polygon [p distance]
  (trimmed (offset-edges p distance)))
