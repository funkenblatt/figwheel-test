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

   ;; Level 7
   [{:type :figwheel-test.snake/line, :p1 [-110 43], :p2 [-70 111]}
    {:type :figwheel-test.snake/line, :p1 [-70 111], :p2 [26 145]}
    {:type :figwheel-test.snake/line, :p1 [26 145], :p2 [171 135]}
    {:type :figwheel-test.snake/line, :p1 [171 135], :p2 [234 81]}
    {:type :figwheel-test.snake/line, :p1 [234 81], :p2 [381 46]}
    {:type :figwheel-test.snake/line, :p1 [381 46], :p2 [422 65]}
    {:type :figwheel-test.snake/line, :p1 [422 65], :p2 [451 138]}
    {:type :figwheel-test.snake/line, :p1 [451 138], :p2 [378 276]}
    {:type :figwheel-test.snake/line, :p1 [378 276], :p2 [197 282]}
    {:type :figwheel-test.snake/line, :p1 [197 282], :p2 [24 261]}
    {:type :figwheel-test.snake/line, :p1 [24 261], :p2 [-160 205]}
    {:type :figwheel-test.snake/line, :p1 [-160 205], :p2 [-230 72]}
    {:type :figwheel-test.snake/line, :p1 [-230 72], :p2 [-265 -79]}
    {:type :figwheel-test.snake/line, :p1 [-265 -79], :p2 [-151 -185]}
    {:type :figwheel-test.snake/line, :p1 [-151 -185], :p2 [4 -215]}
    {:type :figwheel-test.snake/line, :p1 [4 -215], :p2 [136 -205]}
    {:type :figwheel-test.snake/line, :p1 [136 -205], :p2 [206 -170]}
    {:type :figwheel-test.snake/line, :p1 [206 -170], :p2 [307 -193]}
    {:type :figwheel-test.snake/line, :p1 [307 -193], :p2 [375 -266]}
    {:type :figwheel-test.snake/line, :p1 [-1 -109], :p2 [98 -100]}
    {:type :figwheel-test.snake/line, :p1 [98 -100], :p2 [146 -90]}
    {:type :figwheel-test.snake/line, :p1 [146 -90], :p2 [212 -17]}
    {:type :figwheel-test.snake/line, :p1 [212 -17], :p2 [260 -4]}
    {:type :figwheel-test.snake/line, :p1 [260 -4], :p2 [369 -37]}
    {:type :figwheel-test.snake/line, :p1 [369 -37], :p2 [466 -28]}
    {:type :figwheel-test.snake/line, :p1 [466 -28], :p2 [535 57]}
    {:type :figwheel-test.snake/line, :p1 [535 57], :p2 [561 187]}
    {:type :figwheel-test.snake/line, :p1 [561 187], :p2 [483 313]}
    {:type :figwheel-test.snake/line, :p1 [483 313], :p2 [375 368]}
    {:type :figwheel-test.snake/line, :p1 [308 -115], :p2 [428 -153]}
    {:type :figwheel-test.snake/line, :p1 [428 -153], :p2 [487 -223]}
    {:type :figwheel-test.snake/line, :p1 [487 -223], :p2 [509 -340]}
    {:type :figwheel-test.snake/line, :p1 [509 -340], :p2 [479 -395]}
    {:type :figwheel-test.snake/line, :p1 [479 -395], :p2 [288 -425]}
    {:type :figwheel-test.snake/line, :p1 [288 -425], :p2 [253 -311]}
    {:type :figwheel-test.snake/line, :p1 [253 -311], :p2 [138 -280]}
    {:type :figwheel-test.snake/line, :p1 [138 -280], :p2 [8 -290]}
    {:type :figwheel-test.snake/line, :p1 [8 -290], :p2 [-142 -300]}
    {:type :figwheel-test.snake/line, :p1 [-142 -300], :p2 [-265 -270]}
    {:type :figwheel-test.snake/line, :p1 [-265 -270], :p2 [-360 -197]}
    {:type :figwheel-test.snake/line, :p1 [-364 -16], :p2 [-319 171]}
    {:type :figwheel-test.snake/line, :p1 [-319 171], :p2 [-247 288]}
    {:type :figwheel-test.snake/line, :p1 [-247 288], :p2 [-94 369]}
    {:type :figwheel-test.snake/line, :p1 [-94 369], :p2 [102 364]}
    {:type :figwheel-test.snake/line, :p1 [102 364], :p2 [158 383]}
    {:type :figwheel-test.snake/line, :p1 [-289 -161], :p2 [-378 -95]}
    {:type :figwheel-test.snake/line, :p1 [-378 -95], :p2 [-477 -44]}
    {:type :figwheel-test.snake/line, :p1 [-477 -44], :p2 [-476 119]}
    {:type :figwheel-test.snake/line, :p1 [-476 119], :p2 [-418 263]}
    {:type :figwheel-test.snake/line, :p1 [-563 25], :p2 [-551 -175]}
    {:type :figwheel-test.snake/line, :p1 [-551 -175], :p2 [-394 -271]}
    {:type :figwheel-test.snake/line, :p1 [-394 -271], :p2 [-335 -369]}
    {:type :figwheel-test.snake/line, :p1 [-335 -369], :p2 [-222 -409]}]

   ;; Level 8
   [{:type :figwheel-test.snake/line, :p1 [-60 -36], :p2 [157 -37]}
    {:type :figwheel-test.snake/line, :p1 [-70 50], :p2 [137 50]}
    {:type :figwheel-test.snake/line, :p1 [261 -112], :p2 [261 104]}
    {:type :figwheel-test.snake/line, :p1 [41 188], :p2 [352 186]}
    {:type :figwheel-test.snake/line, :p1 [376 -190], :p2 [375 81]}
    {:type :figwheel-test.snake/line, :p1 [-130 -176], :p2 [209 -176]}
    {:type :figwheel-test.snake/line, :p1 [-183 -95], :p2 [-180 145]}
    {:type :figwheel-test.snake/line, :p1 [-224 -177], :p2 [-466 -356]}
    {:type :figwheel-test.snake/line, :p1 [-242 -47], :p2 [-493 -47]}
    {:type :figwheel-test.snake/line, :p1 [-241 49], :p2 [-558 49]}
    {:type :figwheel-test.snake/line, :p1 [-486 102], :p2 [-485 379]}
    {:type :figwheel-test.snake/line, :p1 [-401 107], :p2 [-401 366]}
    {:type :figwheel-test.snake/line, :p1 [-347 248], :p2 [-49 250]}
    {:type :figwheel-test.snake/line, :p1 [-49 250], :p2 [-49 159]}
    {:type :figwheel-test.snake/line, :p1 [141 240], :p2 [141 413]}
    {:type :figwheel-test.snake/line, :p1 [252 240], :p2 [252 413]}
    {:type :figwheel-test.snake/line, :p1 [405 143], :p2 [607 345]}
    {:type :figwheel-test.snake/line, :p1 [382 265], :p2 [555 426]}
    {:type :figwheel-test.snake/line, :p1 [179 -272], :p2 [393 -422]}
    {:type :figwheel-test.snake/line, :p1 [334 -249], :p2 [545 -383]}
    {:type :figwheel-test.snake/line, :p1 [-95 -239], :p2 [-96 -407]}
    {:type :figwheel-test.snake/line, :p1 [34 -241], :p2 [33 -404]}
    {:type :figwheel-test.snake/line, :p1 [-397 -99], :p2 [-397 -218]}]
   ]
  )
