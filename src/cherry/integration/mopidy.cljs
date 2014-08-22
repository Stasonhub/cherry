(ns cherry.integration.mopidy
  "Integration with a Mopidy server http://www.mopidy.com/"
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan timeout close! pipe]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(def WebSocket (js/require "ws"))

(defn debug [& args]
  (apply util/debug "[mopidy]" args))

(defn log [& args]
  (apply println "[mopidy]" args))

(defn connect!
  "Returns a chan containing (cons :connected msgs)"
  [ws-url in]
  (let [out (chan)
        connected (chan)
        conn (doto (WebSocket. ws-url)
               (.on "open" (fn []
                             (log "connected to mopidy")
                             (put! connected :connected)
                             (close! connected)))
               (.on "message" (fn [msg]
                                (debug "got" msg)
                                (put! out (js->clj
                                           (.parse js/JSON msg)
                                           :keywordize-keys true))))
               (.on "error" (fn [e]
                              (log "ERROR: couldn't connect to mopidy" e))))]
    (go (<! connected)
        (loop []
          (let [msg (<! in)]
            (debug "sending to mopidy" msg)
            (.send conn msg)
            (recur))))
    out))

(defn string->json-rpc
  "Takes a chan of strings, returns a chan of JSON-RPC commands"
  [in]
  (let [out (chan)]
    (go-loop []
      (let [m (<! in)
            x (clj->js {:jsonrpc "2.0" :id 1 :method m})]
        (>! out (.stringify js/JSON x))
        (recur)))
    out))

(defn ^:export init [ec]
  (let [<-ec (chan)
        ->mopidy (chan)
        ->ec (->> ->mopidy
                  string->json-rpc
                  (connect! (.-config.mopidy_url ec)))]
    (.consume ec (fn [msg] (put! <-ec msg)))

    (go-loop []
      (let [msg (<! ->ec)]
        (.produce ec (clj->js {:from "music" :body msg}))
        (recur)))

    (go-loop []
      (let [msg (-> (<! <-ec) (js->clj :keywordize-keys true))]
        (when (-> msg :to keyword (= :music))
          (put! ->mopidy (:body msg)))
        (recur)))))
