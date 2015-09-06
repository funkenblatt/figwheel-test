(ns figwheel-test.geometry)

(defn v- [[ax ay] [bx by]] (array (- ax bx) (- ay by)))
(defn v+ [[ax ay] [bx by]] (array (+ ax bx) (+ ay by)))
(defn vscale [s [ax ay]] (array (* s ax) (* s ay)))

(defn vdot [[ax ay] [bx by]]
  (+ (* ax bx) (* ay by)))

(defn vcross [[ax ay] [bx by]]
  (- (* ax by) (* bx ay)))

(defn vsquare [v] (vdot v v))

(defn vmag [[x y]] (.sqrt js/Math (+ (* x x) (* y y))))

(defn v= [[ax ay] [bx by]] (and (= ax bx) (= ay by)))

(defn normalized [v] (vscale (/ 1 (vmag v)) v))

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
        det (- (* -1 dax dby) (* -1 day dbx))]
    (when (not= det 0)
      (let [s (/ (+ (* dby abx -1) (* dbx aby)) det)
            t (/ (+ (* -1 day abx) (* dax aby)) det)]
        [s t]))))

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

(defn unit-vector [angle]
  (array (js/Math.cos angle) (js/Math.sin angle)))

(defn arc-intersection
  "Return a vector [a1 a2 b1 b2] of angles at which the given arcs
  A and B would intersect, or nil if they end up not intersecting."
  [ca ra cb rb]
  (let [[dx dy :as delta-c] (v- cb ca)
        c2 (vdot delta-c delta-c)
        costhb (/ (+ c2 (* rb rb) (- (* ra ra)))
                  (* 2 rb (js/Math.sqrt c2)))]
    (when (<= (js/Math.abs costhb) 1)
      (let [thb (js/Math.acos costhb)
            tha (js/Math.asin (* (/ rb ra) (js/Math.sin thb)))
            base-angle (js/Math.atan2 dy dx)]
        [(+ base-angle tha)
         (- base-angle tha)
         (+ base-angle js/Math.PI (- thb))
         (+ base-angle js/Math.PI thb)]))))

(defn vec-angle [[x y]]
  (js/Math.atan2 y x))

(defn line-arc-intersect [r c p1 p2]
  (let [dp (v- p2 p1)
        dp2 (vdot dp dp)
        dc (v- p1 c)
        dc2 (vdot dc dc)
        discr (-> (vdot dp dc)
                  (js/Math.pow 2)
                  (- (* dp2 (- dc2 (* r r)))))]
    (if (>= discr 0)
      (let [sqdiscr (js/Math.sqrt discr)
            t1 (-> (- (vdot dp dc)) (- sqdiscr) (/ dp2))
            t2 (-> (- (vdot dp dc)) (+ sqdiscr) (/ dp2))
            th1 (-> (vscale t1 dp) (v+ dc) vec-angle)
            th2 (-> (vscale t2 dp) (v+ dc) vec-angle)]
        [t1 t2 th1 th2])
      nil)))

(defn closest-approach
  "Find the distance between p and the point on the line segment p1-p2 closest to p"
  [[p1 p2] p]
  (let [dp (v- p2 p1)
        s (-> (vdot (v- p1 p) dp) (/ (vdot dp dp)) (* -1))
        s (max 0 (min 1 s))]
    (vmag (v- (v+ p1 (vscale s dp)) p))))
