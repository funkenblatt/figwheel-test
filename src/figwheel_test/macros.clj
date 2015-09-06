(ns figwheel-test.macros
  (:require clojure.string))

(defmacro cps-let [[v expr :as bindings] & body]
  (if (empty? bindings)
    `(do ~@body)
    `(let [~'! (fn ~(if (symbol? v) [v] v)
                 (cps-let 
                  ~(vec (drop 2 bindings))
                  ~@body))]
       ~expr)))

(def math-pattern #"\+|-|\*\*|\*")

(defn parse-mathvar [v]
  (let [[init-arg & args] (->>
                           (clojure.string/split (name v) math-pattern)
                           (map read-string))]
    (loop [out init-arg
           args args
           ops (re-seq math-pattern (name v))]
      (if (not-empty args)
        (recur (list (symbol (first ops)) out (first args))
               (rest args) (rest ops))
        out))))

(comment
  (parse-mathvar 'x**2+2) => (+ (** x 2) 2))

(defmacro math-vars [& form]
  (let [math-pred #(and (symbol? %)
                        (nil? (namespace %))
                        (re-find math-pattern (name %))
                        (re-find #"[a-zA-Z]" (name %)))
        math-vars (clojure.walk/postwalk
                   (fn [x]
                     (cond (and (symbol? x) (math-pred x)) [x]
                           (coll? x) (mapcat identity x)))
                   form)]
    `(let [~@(for [v math-vars x [v (parse-mathvar v)]] x)]
       ~@form)))

(defmacro nlet
  "Scheme-style named let"
  [name bindings & body]
  (let [bindings (partition 2 bindings)]
    `((fn ~name ~(mapv first bindings)
        ~@body)
      ~@(map second bindings))))
