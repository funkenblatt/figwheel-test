(ns figwheel-test.canvas)

(defn lines [ctx & [[x0 y0] & _ :as points]]
  (.beginPath ctx)
  (.. ctx (moveTo x0 y0))
  (doseq [[x y] points] (.. ctx (lineTo x y))))

(defn stroke-lines [ctx & args]
  (apply lines ctx args)
  (.stroke ctx))

(defn stroke-circle [ctx [cx cy] r]
  (.beginPath ctx)
  (.arc ctx cx cy r 0 (* 2 js/Math.PI))
  (.stroke ctx))

(defn stroke-arc [ctx [cx cy] r th1 th2]
  (.beginPath ctx)
  (.arc ctx cx cy r th1 th2)
  (.stroke ctx))

(defn clear [ctx]
  (let [can (.-canvas ctx)]
    (.clearRect ctx 0 0 (.-width can) (.-height can))))

(defn with-saved-context [ctx fun]
  (.save ctx)
  (try (fun)
       (finally (.restore ctx))))

(defn set-stroke [ctx style]
  (set! (.-strokeStyle ctx) style))

(defn elem-offset [elem]
  (loop [e elem
         x (.-offsetLeft elem)
         y (.-offsetTop elem)]
    (if-let [p (.-offsetParent e)]
      (recur p (+ x (.-offsetLeft p)) (+ y (.-offsetTop p)))
      (array x y))))

(defn canvas-coord [ctx evt]
  (let [can (.-canvas ctx)
        [x y] (elem-offset can)]
    (array (- (.-pageX evt) x)
           (- (.-pageY evt) y))))
