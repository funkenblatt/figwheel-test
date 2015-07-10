(ns figwheel-test.snake-levels)

(def levels
  [
   ;; Level 1
   [{:type :figwheel-test.snake/line, :p1 [-415 44], :p2 [-415 -313]}
    {:type :figwheel-test.snake/line, :p1 [-415 -313], :p2 [-55 -313]}
    {:type :figwheel-test.snake/line, :p1 [-51 246], :p2 [377 246]}
    {:type :figwheel-test.snake/line, :p1 [377 246], :p2 [377 -200]}]

   ;; Level 2
   [{:type :figwheel-test.snake/line, :p1 [-344 341], :p2 #js [-469 341]}
    {:type :figwheel-test.snake/line, :p1 #js [-469 341], :p2 #js [-469 -292]}
    {:type :figwheel-test.snake/line, :p1 #js [-469 -292], :p2 #js [-327 -292]}
    {:type :figwheel-test.snake/line, :p1 [-189 340], :p2 #js [261 340]}
    {:type :figwheel-test.snake/line, :p1 [-203 -294], :p2 #js [261 -294]}
    {:type :figwheel-test.snake/line, :p1 [409 -293], :p2 #js [541 -293]}
    {:type :figwheel-test.snake/line, :p1 #js [541 -293], :p2 #js [541 327]}
    {:type :figwheel-test.snake/line, :p1 #js [541 327], :p2 #js [420 327]}]

   ;; Level 3
   [{:type :figwheel-test.snake/line, :p1 [-487 145], :p2 [-487 -206]}
    {:type :figwheel-test.snake/line, :p1 [-487 -206], :p2 #js [-241 -452]}
    {:type :figwheel-test.snake/line, :p1 #js [-241 -452], :p2 [-5 -452]}
    {:type :figwheel-test.snake/line, :p1 [-5 -452], :p2 [-5 -280]}
    {:type :figwheel-test.snake/line, :p1 [-190 399], :p2 [153 399]}
    {:type :figwheel-test.snake/line, :p1 [153 399], :p2 [461 399]}
    {:type :figwheel-test.snake/line, :p1 [461 399], :p2 [461 120]}
    {:type :figwheel-test.snake/line, :p1 [461 120], :p2 [261 120]}
    {:type :figwheel-test.snake/line, :p1 [318 -426], :p2 #js [530 -214]}
    {:type :figwheel-test.snake/line, :p1 [-508 422], :p2 #js [-235 149]}]
   ])
