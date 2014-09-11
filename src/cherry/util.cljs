(ns cherry.util)

(def debug? (boolean (.-env.DEBUG js/process)))
(def util (js/require "util"))

(defn trace
  ([x]
     (trace "" x))
  ([tag x]
     (println "TRACE:" tag x)
     x))

(defn debug [& args]
  (when debug? (apply println args)))
