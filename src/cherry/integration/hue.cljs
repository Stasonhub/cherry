(ns cherry.integration.hue
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(def util (js/require "util"))
(def request (js/require "request"))

(defn debug [& args]
  (apply util/debug "[Hue]" args))

(defn log [& args]
  (apply println "[Hue]" args))

(defn hue! [hue-url user opts]
  (let [out (chan 1)
        lights (cond (:lights opts) (:lights opts)
                     (:light opts) [(:light opts)]
                     :else [1 2])
        opts (clj->js (dissoc opts :light))
        body (js/JSON.stringify opts)]
    (doseq [light lights]
      (let [url (str hue-url "/api/" user "/lights/" light "/state")]
        (debug "sending to Hue" url body)
        (request. #js {:method "PUT"
                       :url url
                       :body body
                       :json true}
                  (fn [err res body]
                    (cond err (put! out err)

                      (and res body (= (.-statusCode res) 200))
                      (put! out (js->clj body :keywordize-keys true))

                      :else (put! out (js/Error. (str "Hue replied with " (.inspect util res)))))
                    (close! out)))))
    out))

(defn ^:export init [ec]
  (let [<-ec (chan)]
    (.consume ec (fn [x] (put! <-ec (js->clj x :keywordize-keys true))))

    (go-loop []
      (let [msg (<! <-ec)]
        (when (-> msg :to keyword (= :lights))
          (hue! (.-config.hue_host ec) (.-config.hue_user ec) (:body msg)))
        (recur)))))
