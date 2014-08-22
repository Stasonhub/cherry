(ns cherry.integration.myo
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(defn log [& args]
  (apply println "[Myo]" args))

(def WebSocket (js/require "ws"))

(defn ^:export init [ec]
  (let [ch (chan)
        ->ec (chan)
        conn (doto (WebSocket. "ws://localhost:7204/myo/1")
               (.on "open" (fn [] (log "connected to Myo Connect")))
               (.on "message" (fn [data flags]
                                (put! ch (-> data
                                             (js/JSON.parse)
                                             (js->clj :keywordize-keys true))))))]

    (go-loop []
      (let [x (<! ->ec)]
        (.produce ec (clj->js x))
        (recur)))

    (go-loop []
      (let [[msg-type msg :as foo] (<! ch)]
        (case (:type msg)
          "pose"
          (let [pose (:pose msg)]
            (log "got pose:" pose)
            (put! ->ec {:from "gesture" :body (:pose msg)}))
          nil)
        (recur)))))
