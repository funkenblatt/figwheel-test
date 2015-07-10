(ns figwheel-test.macros)

(defmacro cps-let [[v expr :as bindings] & body]
  (if (empty? bindings)
    `(do ~@body)
    `(let [~'! (fn ~(if (symbol? v) [v] v)
                 (cps-let 
                  ~(vec (drop 2 bindings))
                  ~@body))]
       ~expr)))
