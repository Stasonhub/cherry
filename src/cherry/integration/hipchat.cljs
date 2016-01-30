(ns cherry.integration.hipchat
  "Integration with Hipchat through xmpp"
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(def util (js/require "util"))
(def xmpp (js/require "node-xmpp-client"))

(defn debug [& args]
  (apply util/debug "[HipChat]" args))

(defn log [& args]
  (apply println "[HipChat]" args))

(defn create-elt
  [name data]
  (xmpp.Element. name (clj->js data)))

(defn on-connected
  [cl room+id ->hipchat]
  (fn [data]
    (log "connected as" (-> data .-jid .toString))
    (-> (create-elt "presence" {:type "available"}) (.c "show") (.t "chat")
        (->> (.send cl)))
    (-> (create-elt "presence" {:to room+id})
        (.c "x" (clj->js {:xmlns "http://jabber.org/protocol/muc"}))
        (->> (.send cl)))

    (go-loop []
      ;; wait 1.5s before sending message, otherwise server might reject
      (<! (timeout 1500))
      (let [msg (<! ->hipchat)]
        (debug "sending xmpp:" msg)
        (-> (create-elt "message" {:to room+id :type "groupchat"})
            (.c "body")
            (.t msg)
            (->> (.send cl)))
        (recur)))

    (js/setInterval (fn [] (.send cl " ")) 30000)))

(defn on-stanza
  [room+id ->ec]
  (fn [stz]
    (cond
     (and (.is stz "message") (= "error" (.-attrs.type stz)))
     (log "xmpp error while sending"
          (str "\"" (-> stz (.getChild "body") .getText) "\":")
          (-> stz (.getChild "error") (.getChild "text") .getText))

     (and (.is stz "message")
          (= (-> stz .-attrs .-type) "groupchat")
          (not= (-> stz .-attrs .-from) room+id))
     (when-let [body (.getChild stz "body")]
       (let [body-str (.getText body)]
         (when (seq body-str)
           (put! ->ec body-str)))))))

(defn connect!
  "in: chan containing chatroom messages"
  [jid pwd room+id ->ec ->hipchat]
  (let [cl (xmpp.Client. (clj->js {:jid (str jid "/bot") :password pwd}))]
    (.on cl "online" (on-connected cl room+id ->hipchat))
    (.on cl "error" (fn [e] (log "xmpp error" e)))
    (.on cl "stanza" (on-stanza room+id ->ec))))

(defn ^:export init [ec]
  (let [<-ec (chan)
        ->ec (chan)
        ->hipchat (chan 100)]
    (.consume ec (fn [msg] (put! <-ec msg)))

    ;; from hipchat to ec
    (go-loop []
      (let [msg (<! ->ec)]
        (.produce ec (clj->js {:from "chat" :body msg}))
        (recur)))

    ;; from ec to hipchat
    (go-loop []
      (let [msg (-> (<! <-ec) (js->clj :keywordize-keys true))]
        (when (-> msg :to keyword (= :chat))
          (debug "sending" msg)
          (put! ->hipchat (:body msg)))
        (recur)))

    (connect! (.-config.hipchat_jid ec)
              (.-config.hipchat_pwd ec)
              (.-config.hipchat_room ec)
              ->ec
              ->hipchat)))
