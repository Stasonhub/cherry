(ns cherry.core
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]]
            [cherry.integration.mopidy :as mopidy]
            [cherry.integration.hipchat :as hipchat]
            [cherry.integration.wit :as wit]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(def util (js/require "util"))
(def fs (js/require "fs"))
(def path (js/require "path"))

(nodejs/enable-util-print!)

(let [re #"\.js$"]
  (defn js? [p]
    (boolean (re-find re p))))

(defn load-modules!
  "Load modules from a config file and hook them to the firehose"
  [config firehose]
  (let [pwd (.-pwd config)
        modules (.-plugins config)
        mult (async/mult firehose)]
    (doseq [m modules
            :let [js-path (str pwd "/" m)]]
      (println "> Loading" m)
      (if (and (js? m) (not (.existsSync fs js-path)))
        (println "ERROR: could not find" js-path)
        (let [f (if (js? m)
                  (js/require js-path)
                  (aget (js/eval m) "init"))]
          (f #js {:consume (fn [f]
                             (let [ch (chan)]
                               (async/tap mult ch)
                               (go-loop []
                                 (let [[sender msg] (<! ch)]
                                   (when (and (not (nil? msg))
                                              (not (= m sender)))
                                     (f (clj->js msg)))
                                   (recur)))))
                  :config config
                  :produce (fn [x]
                             (println ">" (str m ":") x)
                             (put! firehose [m x]))}))))))

(defn load-config! [p]
  (let [dirname (.dirname path p)]
    (doto (-> (.readFileSync fs p) (js/JSON.parse))
      (aset "pwd" (->> (.join path (.cwd js/process) dirname)
                       (.normalize path))))))

(defn usage! []
  (println "usage:" (first (.-argv js/process)) "<path/to/config.json>")
  (js/process.exit 1))

(defn -main [& [p args]]
  (cond (not p)
        (usage!)

        :else
        (let [conf (load-config! p)
              port (or (.-port conf) 4433)
              firehose (chan)]
          (println "cherry starting up on port" port)
          (load-modules! conf firehose))))

(set! *main-cli-fn* -main)
