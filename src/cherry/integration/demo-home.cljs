(ns cherry.integration.demo-home
  "Start a WS Server and send request from :mic into the socket "
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close! mult tap]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(defn debug [& args]
  (apply util/debug "[demo]" args))

(defn log [& args]
  (apply println "[demo]" args))

(def WebSocketServer (.-Server (js/require "ws")))

(defn connect!
  [demo-page-port ->demo-page ->ec]
  (let [fmt (juxt (comp :intent first :outcomes)
                  (comp :entities first :outcomes)
                  identity)
        mult (async/mult ->demo-page)]
    (log "starting ws server at" demo-page-port)
    (doto (WebSocketServer. #js {:port demo-page-port})
      (.on "connection" (fn [ws]
                          (let [ch (chan)]
                            (.on ws "close" (fn [ws]
                                              (debug "closed ws")
                                              (async/untap mult ch)))
                            (async/tap mult ch)
                            (go-loop []
                              (let [msg (<! ch)]
                                (debug "sending" (boolean ws) msg)
                                (when ws
                                  (-> msg
                                      fmt
                                      clj->js
                                      js/JSON.stringify
                                      (->> (.send ws)))
                                  (recur))))

                            (.on ws "message"
                                 (fn [msg]
                                   (try (let [x (js/JSON.parse msg)]
                                          (put! ->ec x))
                                        (catch :default e
                                          (log "could not parse" msg))))))))
      (.on "error" (fn [err]
                     (log "error" err)))

      )))

(defn consume-wit-command
  [wit-body ->demo-page]
  (put! ->demo-page wit-body))

(defn ^:export init [ec]
  (go (let [<-ec (chan)
            ->ec (chan)
            ->demo-page (chan)]
        (.consume ec (fn [msg] (put! <-ec msg)))

        (go-loop []
          (let [msg (<! ->ec)]
            (.produce ec (clj->js msg))
            (recur)))

        (go-loop []
          (let [msg (-> (<! <-ec)
                        (js->clj :keywordize-keys true))]
            (log "consuming" msg)
            (case (keyword (:from msg))
              :wit
              (consume-wit-command (:body msg) ->demo-page)

              nil)
            (recur)))

        (connect! (.-config.demo_port ec) ->demo-page ->ec))))
