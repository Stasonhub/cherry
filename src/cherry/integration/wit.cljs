(ns cherry.integration.wit
  "Connect to witd (https://github.com/wit-ai/witd) locally"
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(def http (js/require "http"))
(def util (js/require "util"))
(def request (js/require "request"))

(defn debug [& args]
  (apply util/debug "[witd]" args))

(defn log [& args]
  (apply println "[witd]" args))

(defn wit
  ([cfg url] (wit url {}))
  ([cfg url qs]
   (let [out (chan 1)
         witd-url (.-witd_url cfg)
         access-token (.-wit_token cfg)]
     (request. #js {:url (str witd-url url)
                    :qs (clj->js (merge {:access_token access-token} qs))
                    :json true}
               (fn [err res body]
                 (debug err (when res (.-statusCode res)) (when res (.-body res)) body)

                 (cond err
                       (put! out err)

                      (and res (= (.-statusCode res) 200))
                      (when body (put! out (js->clj body :keywordize-keys true)))

                      :else (put! out (js/Error. (str "Wit replied with " (.inspect util res)))))
                 (close! out)))
     out)))

(defn interpret [cfg q]
  (debug "interpreting" q)
  (go (case q
        "start" (wit cfg "/audio" {:action "start"
                                   :access_token (.-wit_token cfg)})
        "stop" (<! (wit cfg "/audio" {:action "stop"}))
        (let [wit-body (<! (wit cfg "/text" {:q q}))]
          wit-body))))

(defn ^:export init [ec]
  (let [<-ec (chan)
        ->ec (chan)
        cfg (.-config ec)]
    (.consume ec (fn [msg] (put! <-ec msg)))

    (go-loop []
      (let [msg (<! ->ec)]
        (.produce ec (clj->js msg))
        (recur)))

    (go-loop []
      (let [msg (-> (<! <-ec)
                    (js->clj :keywordize-keys true))]
        (when (-> msg :to keyword (= :wit))
          (when-let [x (<! (interpret cfg (:body msg)))]
            (put! ->ec {:from "wit" :body x})))
        (recur)))))
