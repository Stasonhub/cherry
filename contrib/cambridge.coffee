module.exports = (cherry) ->
  p = cherry.plugins()

  console.log p
  setTimeout (-> p.spop "play"), 1000

  intents =
    bot_hello: -> p.tts "Good day to you!"
    bot_name:  -> p.tts "My name is Samantha."
    bot_sport: -> p.tts "My favorite sport is volleyball. I quite like soccer too."
    bot_maker: -> p.tts "I have been made in Palo Alto, by the Wit team"
    pause: -> p.spop 'pause'
    resume: -> p.spop 'resume'
    party: ->
      p.spop 'uplay spotify:track:1CNJyTUh56oj3OCZOZ5way'
      setTimeout (-> p.spop "seek", seconds: 58200), 75
      p.hue(on: true, alert: 'lselect', effect: 'colorloop')
    lights: (x) ->
      opts = wit_to_hue(entities)
# (p! ->ec {:to "chat" :body (str "Hue: " (pr-str opts))}))
      p.hue(opts)
    music_play: -> p.spop('play')
    music_stop: -> p.spop('stop')
    music_next: -> p.spop('next')
    music_previous: -> p.spop('prev')
    music_status: -> p.spop('status')
    music_qclear: -> p.spop "queue_clear"
    music_qlist: -> p.spop "queue_ls"
    music_search: (entities) -> p.spop "search", query: entities.music?[0].value
    music_enqueue_from_search: ->

  cherry.handle
    chat: (x) -> p.wit(x) # send all chats to wit
    pin: (x) ->
      if x.state == 'low'
        p.hue(on: false)
        # p.wit('stop')
      else
        p.hue(on: true)
        # p.wit('start')
    # spop: (x) -> p.hipchat(JSON.stringify(x))
    myo: (x) ->
      if x == 'wave_in'
        p.hue(on: true, lights: [1, 2])
      else if x == 'wave_out'
        p.hue(on: false, lights: [1, 2])
      else
        console.log('unknown myo: ' + x)
    wit: (x) ->
      return if !x.outcomes

      outcome = x.outcomes[0]
      intent = outcome.intent
      entities = outcome.entities

      if f = intents[intent]
        f(entities)
      else
        console.log 'unknown intent', intent

###
        "music_enqueue_from_search" (let [num (-> entities :number first :value)]
                                      (if (pos? num)
                                        (->music! "queue_add" {:num num})
                                        (log (str "error while adding: "
                                                  num "is not a valid number"))))
        ;; "music_image" (->music! "image")
        "music_goto" (let [num (-> entities :number first :value)]
                       (if (pos? num)
                         (->music! "queue_goto" {:num num})
                         (log (str "error while adding: "
                                   num "is not a valid number"))))
        (do (log (str "did not understand intent " intent))
            (->tts! "I did not understand that"))))
###
