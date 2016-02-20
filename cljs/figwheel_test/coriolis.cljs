(ns figwheel-test.coriolis
  (:require [figwheel-test.geometry :as g]
            [figwheel-test.canvas :as c]
            [figwheel-test.common :refer [with-viewport ctx tau] :as com])
  (:require-macros [figwheel-test.macros :as m]))

(def dt (/ 1.0 60))

(defn update-state [{:as state
                     :keys [px pv th w n
                            x-history]}]
  

  (-> (if (= n 0)
        (update state :x-history conj (g/rotate px (- th)))
        state)
      (update :n com/modinc 5)
      (update :th + (* w dt))
      (update :px g/v+ (g/vscale dt pv))))

(defn draw-state [{:keys [px th x-history
                          xstart
                          frame]}]
  (with-viewport
    (fn []
      (let [s (com/scale-factor)]
        (set! (.-lineWidth ctx) (/ 2 s))
        (.scale ctx s s))
      (set! (.-strokeStyle ctx) "#f83")
      (c/with-saved-context ctx
        (fn []
          (if (not= frame :rotating)
            (.rotate ctx th))
          (apply c/stroke-lines ctx (conj x-history (g/rotate px (- th))))
          ;; (run! (fn [x] (c/stroke-circle ctx x 1)) x-history)
          ))
      (when (= frame :rotating)
        (.rotate ctx (- th)))
      (c/stroke-circle ctx [0 0] 200)
      
      (c/stroke-lines ctx
                      (g/vscale
                       -200 (g/unit-vector (+ th (/ com/tau 4))))
                      (g/vscale
                       200 (g/unit-vector (+ th (/ com/tau 4)))))
      (c/stroke-circle ctx px 2)

      (c/stroke-lines
       ctx (g/vscale -200 (g/unit-vector th)) (g/vscale 200 (g/unit-vector th)))
      (set! (.-strokeStyle ctx) "#000")
      (c/stroke-lines ctx xstart px)
      (c/stroke-lines ctx [-200 0] [200 0])
      (c/stroke-lines ctx [0 -200] [0 200]))
    true))

(def init-state {:px [0 -100]
                 :pv [(* 100 0.8) 80]
                 :xstart [0 -100]
                 :th 0
                 :n 0
                 :w 0.8
                 :x-history []})

(def state (atom (assoc init-state :frame :rotating)))

(defn show-stuff [init cont]
  (swap! state merge init)
  (set! (.-onclick com/canvas)
        (fn [] (swap! state update 
                      :frame {:rotating :still
                              :still :rotating})))
  (m/nlet lp []
    (if (<= (g/vmag (:px @state)) 200)
      (do
        (draw-state @state)
        (swap! state update-state)
        (js/window.requestAnimationFrame lp))
      (do
        (set! (.-onclick com/canvas) nil)
        (cont nil)))))

(defn size-canvas []
  (set! (.-height com/canvas) (min (* js/window.innerHeight 0.8) 960))
  (set! (.-width com/canvas) (min (* js/window.innerWidth 0.8) 1280))
  (draw-state init-state))

(defn init-everything []
  (com/init-elements)
  (size-canvas)
  (set! js/window.onresize size-canvas)
  (.insertBefore 
   js/document.body 
   (hipo.core/create 
    [:div#controls
     [:p "Any serious long term space voyage is probably going to want
some sort of rotating crew compartment in order to simulate gravity.  But
spinning isn't quite like gravity - it will introduce other weird effects,
like the Coriolis force."]
     [:p "The animation here illustrates how the motion of an object
thrown straight upwards (from a rotating perspective) would proceed.  The
orange axes show the rotating frame of reference's coordinate axes, and the
orange curve shows the path of the object in the rotating reference frame.
The black axes and line show the motion from a stationary observer's perspective.
As you will see, what appears to be a straight-line motion in a stationary frame
turns into a curved motion in the rotating frame, which ends up being forced
to the right initially and then to the left as it \"falls.\""]
     [:p "Press the 'Show' button to begin the animation.  
While the animation is playing, you can click on it to switch
between a rotating and a stationary frame of reference.  Once you're
done here, you can click on the \"Back\" button to return to the interactive
simulation."]
     [:button#start "Show"]
     [:button#play "Back"]])
   com/canvas)

  (try (js/mixpanel.track "start coriolis")
       (catch js/Error e))

  (let [button (js/document.getElementById "start")
        play (js/document.getElementById "play")]
    (set! (.-onclick button)
          (fn lp []
            (set! (.-onclick button) nil)
            (set! (.-onclick play) nil)
            (show-stuff init-state 
                        (fn [x] (set! (.-onclick button) lp)
                          (set! (.-onclick play) figwheel-test.platform/run-stuff)))))

    (set! (.-onclick play) figwheel-test.platform/run-stuff)))

(comment
  (init-everything)
  (show-stuff init-state))
