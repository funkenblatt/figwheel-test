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

   ;; Level 4

   [{:type :figwheel-test.snake/line, :p1 [-363 146], :p2 [-30 147]}
    {:type :figwheel-test.snake/line, :p1 [-30 147], :p2 [88 314]}
    {:type :figwheel-test.snake/line, :p1 [88 314], :p2 [332 315]}
    {:type :figwheel-test.snake/line, :p1 [332 315], :p2 [527 51]}
    {:type :figwheel-test.snake/line, :p1 [527 51], :p2 [350 -268]}
    {:type :figwheel-test.snake/line, :p1 [350 -268], :p2 [93 -268]}
    {:type :figwheel-test.snake/line, :p1 [93 -268], :p2 [-34 -110]}
    {:type :figwheel-test.snake/line, :p1 [-34 -110], :p2 [-371 -108]}
    {:type :figwheel-test.snake/line, :p1 [-460 353], :p2 [-65 353]}
    {:type :figwheel-test.snake/line, :p1 [-487 -314], :p2 [-62 -313]}]

   ;; Level 5
   [{:type :figwheel-test.snake/line, :p1 [330 -239], :p2 [331 233]}
    {:type :figwheel-test.snake/line, :p1 [331 233], :p2 [-239 233]}
    {:type :figwheel-test.snake/line, :p1 [-239 233], :p2 [-241 353]}
    {:type :figwheel-test.snake/line, :p1 [-241 353], :p2 [496 352]}
    {:type :figwheel-test.snake/line, :p1 [496 352], :p2 [493 -111]}
    {:type :figwheel-test.snake/line, :p1 [77 -126], :p2 [-368 -126]}
    {:type :figwheel-test.snake/line, :p1 [-368 -126], :p2 [-368 224]}
    {:type :figwheel-test.snake/line, :p1 [-368 224], :p2 [-500 366]}
    {:type :figwheel-test.snake/line, :p1 [-500 366], :p2 [-502 -363]}
    {:type :figwheel-test.snake/line, :p1 [-502 -363], :p2 [-74 -363]}]

   ;; Level 6
   [{:type :figwheel-test.snake/line, :p1 [196 141], :p2 [-295 143]}
    {:type :figwheel-test.snake/line, :p1 [-295 143], :p2 [-420 -180]}
    {:type :figwheel-test.snake/line, :p1 [-420 -180], :p2 [-262 -401]}
    {:type :figwheel-test.snake/line, :p1 [-262 -401], :p2 [195 -403]}
    {:type :figwheel-test.snake/line, :p1 [190 -306], :p2 [-200 -303]}
    {:type :figwheel-test.snake/line, :p1 [-200 -303], :p2 [-302 -171]}
    {:type :figwheel-test.snake/line, :p1 [-302 -171], :p2 [-233 22]}
    {:type :figwheel-test.snake/line, :p1 [-233 22], :p2 [188 21]}
    {:type :figwheel-test.snake/line, :p1 [-48 -98], :p2 [361 -100]}
    {:type :figwheel-test.snake/line, :p1 [361 -100], :p2 [512 102]}
    {:type :figwheel-test.snake/line, :p1 [512 102], :p2 [365 377]}
    {:type :figwheel-test.snake/line, :p1 [365 377], :p2 [-163 382]}
    {:type :figwheel-test.snake/line, :p1 [294 70], :p2 [352 135]}
    {:type :figwheel-test.snake/line, :p1 [352 135], :p2 [262 285]}
    {:type :figwheel-test.snake/line, :p1 [262 285], :p2 [-67 287]}
    {:type :figwheel-test.snake/line, :p1 [-300 432], :p2 [-509 -74]}
    {:type :figwheel-test.snake/line, :p1 [363 -393], :p2 [518 -171]}
    {:type :figwheel-test.snake/line, :p1 [34 -203], :p2 [338 -205]}]
   ]
  )
