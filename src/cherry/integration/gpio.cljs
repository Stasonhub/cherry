(ns cherry.integration.gpio
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(defn debug [& args]
  (apply util/debug "[GPIO]" args))

(defn log [& args]
  (apply println "[GPIO]" args))

(def Gpio (try
            (.-Gpio (js/require "onoff"))
            (catch :default e
                nil)))

(defn ^:export init [ec]
  (if-not Gpio
    (log "No GPIO module found")
    (doseq [[n [dir edge] :as pin] (js->clj (.-config.gpio_pins ec))]
      (let [presses (chan)
            button (Gpio. n dir edge)]
        (log "watching pin" n "with" dir edge)
        (.watch button (fn [err v]
                         (if err
                           (log "error while watching gpio" n "-" err)
                           (put! presses v))))
        (go-loop []
          (let [v (<! presses)
                state (if (pos? v) "high" "low")]
            (debug "pin" n "is" state)
            (.produce ec {:from "pin" :pin n :direction dir :edge edge :body state})
            (recur)))))))
