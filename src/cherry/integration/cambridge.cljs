(ns cherry.integration.cambridge
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]]
            [cherry.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]]))

(def util (js/require "util"))

(defn debug [& args]
  (apply util/debug "[cambridge]" args))

(defn log [& args]
  (apply println "[cambridge]" args))

;; -----------------------------------------------------------------------------
;; Raspberry GPIO

(defn do-pin [{:keys [to from body]} ->ec]
  (cond (= "high" body) (put! ->ec {:to "lights" :body {:on false}})
        (= "low" body) (put! ->ec {:to "lights" :body {:on true}})))

;; -----------------------------------------------------------------------------
;; Wit

(defn wit->hue [intent entities]
  (-> (->> (hash-map :hue (-> entities :color first :value js/parseInt)
                     :alert (-> entities :alert first :value)
                     :effect (-> entities :effect first :value)
                     :light (-> entities :light first :value))
           (filter val)
           (remove (comp (partial = NaN) val))
           (into {}))
      (assoc :on (-> entities :on_off first :value (= "on")))))

(defn do-wit [msg ->ec]
  (let [outcome (-> msg :body :outcomes first)
        intent (:intent outcome)
        entities (:entities outcome)]
    (case intent
      "lights" (let [opts (wit->hue intent entities)]
                 (debug intent "+" entities "=" opts)
                 (put! ->ec {:to "lights" :body opts})
                 (put! ->ec {:to "chat" :body (str "Hue: " (pr-str opts))}))
      "play" (put! ->ec {:to "music" :body "core.playback.play"})
      "pause" (put! ->ec {:to "music" :body "core.playback.pause"})
      "resume" (put! ->ec {:to "music" :body "core.playback.resume"})
      "next" (put! ->ec {:to "music" :body "core.playback.next"})
      "previous" (put! ->ec {:to "music" :body "core.playback.previous"})
      (log (str "did not understand intent " intent)))))

;; -----------------------------------------------------------------------------
;; Mopidy

(defn normalize-mopidy-event [x]
  (->> (hash-map :status (-> x :new_state)
                 :track (-> x :tl_track :track :name)
                 :artists (->> x :tl_track :track :artists (map :name) seq)
                 :album (-> x :tl_track :track :album :name)
                 :event (-> x :event)
                 :volume (-> x :volume))
       (filter val)
       (into {})))

(defn render-status [state]
  (cond
    (and (:track state) (:status state))
    (.format util "[%s] `%s` from %s"
             (:status state) (:track state)
             (.join (clj->js (:artists state)) ", "))

    (= "volume_changed" (:event state))
    (.format util "Volume changed to %s" (:volume state))

    (= "playlists_loaded" (:event state))
    "Playlist loaded"))

(defn state-changed?
  [old new]
  (let [keys [:status :artists :album :track]]
    (not= (select-keys old keys)
          (select-keys new keys))))

(defn render-event [{:keys [event] :as x}]
  (case event
    ("playlists_loaded" "tracklist_changed") nil
    "playback_state_changed" (pr-str ((juxt :old_state :new_state) x))
    "track_playback_started" (let [t (:tl_track x)]
                               (pr-str ((juxt (comp first :artists)
                                              :name) x)))
    "track_playback_paused" (let [t (:tl_track x)]
                               (pr-str ((juxt (comp first :artists)
                                              :name) x)))
    (debug "not handling" x)))

(defn do-mopidy [state msg ->ec]
  (let [mopidy-event (:body msg)
        new-state (merge state (normalize-mopidy-event mopidy-event))
        new-state (if (not= (:status new-state)
                            (:status state))
                    (dissoc new-state :track)
                    new-state)
        body (render-status new-state)
        event (render-event mopidy-event)]
    (debug "mopidy" (state-changed? new-state state) body state new-state)
    ;; (when (and (state-changed? new-state state) body)
    ;;   (put! ->ec {:to "chat" :body body}))
    (when event
      (put! ->ec {:to "chat" :body event}))
    new-state))

;; -----------------------------------------------------------------------------
;; Myo

(defn do-myo [msg ->ec]
  (let [pose (:body msg)]
    (cond (= pose "wave_in") (put! ->ec {:to "hue" :body {:on true :light 2}})
          (= pose "wave_out") (put! ->ec {:to "hue" :body {:on false :light 2}})))
  nil)

;; -----------------------------------------------------------------------------
;; Init

(defn ^:export init [ec]
  (let [<-ec (chan)
        ->ec (chan)]
    (.consume ec (fn [x] (put! <-ec x)))
    (go-loop []
      (let [x (<! ->ec)]
        (.produce ec (clj->js x))
        (recur)))

    (go-loop [state {}]
      (let [msg (js->clj (<! <-ec) :keywordize-keys true)
            _ (debug "read message" msg)
            state' (case (-> msg :from keyword)
                     :pin (do-pin msg ->ec)
                     :chat (put! ->ec {:to "wit" :body (:body msg)})
                     :wit (do-wit msg ->ec)
                     :music (do-mopidy msg state ->ec)
                     :gesture (do-myo msg ->ec)

                     nil)]
        (recur (or state' state))))))
