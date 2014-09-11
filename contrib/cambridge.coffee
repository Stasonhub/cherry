# spop music handling
spop = q: [], cache: []

# from wit output to hue input
wit_to_hue = (entities) ->
  opts = {}
  if x = parseInt(entities.color?[0].value, 10)
    opts.hue = x
  if x = entities.alert?[0].value
    opts.alert = x
  if x = entities.effect?[0].value
    opts.effect = x
  if x = entities.light?[0].value
    opts.light = x
  if x = entities.on_off?[0].value
    opts.on = !!x
  # (-> (->> (hash-map :hue (or (-> entities :color first :value js/parseInt))
  #                    :alert (-> entities :alert first :value)
  #                    :effect (-> entities :effect first :value)
  #                    :light (-> entities :light first :value))
  #          (filter (comp boolean val))
  #          (into {}))
  #     (assoc :on (-> entities :on_off first :value (= "on")))))

  return opts

module.exports = (cherry) ->
  p = cherry.plugins()

  intents =
    bot_hello: -> p.tts "Good day to you!"
    bot_name:  -> p.tts "My name is Samantha."
    bot_sport: -> p.tts "My favorite sport is volleyball. I quite like soccer too."
    bot_maker: -> p.tts "I have been made in Palo Alto, by the Wit team"
    pause: -> p.spop 'toggle'
    resume: -> p.spop 'resume'
    party: ->
      p.spop 'uplay spotify:track:1CNJyTUh56oj3OCZOZ5way'
      setTimeout (-> p.spop "seek", seconds: 58200), 75
      p.hue(on: true, alert: 'lselect', effect: 'colorloop')
    lights: (entities) ->
      # (put! ->ec {:to "chat" :body (str "Hue: " (pr-str opts))}))
      p.hue(wit_to_hue(entities))
    music_play: -> p.spop('play')
    music_stop: -> p.spop('stop')
    music_next: -> p.spop('next')
    music_previous: -> p.spop('prev')
    music_status: -> p.spop('status')
    music_qclear: -> p.spop "qclear"
    music_qlist: -> p.spop "qls"
    ###
    if (!(r && r.tracks && r.tracks.length)) {
      return "There's nothing in the queue";
    }

    var out = '';
    var xs = r.tracks;

    if (xs.length == 1) {
      out += "There is one item in the queue:\n";
    } else {
      out += "There are " + xs.length + " items in the queue:\n";
    }

    out += xs.map(function (x, i) {
      return '' + (i+1) + '. ' + x.title + " by " + x.artist + " (" + x.album + ")";
    }).join('\n');

    return out;
    ###
    music_search: (entities) ->
      item = entities.music?[0].value
      if item
        p.spop 'search "' + item + '"'
      else
        console.log('[spop/search] not an item', item)
    music_goto: (entities) ->
      n = parseInt(entities.number?[0].value || -1, 10)
      if n > 0
        p.spop "goto " + n
      else
        console.log('[spop/goto] not a number', n)
    music_enqueue_from_search: (entities) ->
      n = parseInt(entities.number?[0].value || -1, 10)
      if n > 0 && (uri = spop.cache[n-1]?.uri)
        p.spop "uadd " + uri
      else
        console.log('[spop/enqueue] not in queue', n)

  cherry.handle
    chat: (x) -> p.wit(text: x) # send all chats to wit
    pin: (x) ->
      if x.state == 'low'
        p.hue(on: false)
        p.wit(mic: 'stop')
      else
        p.hue(on: true)
        p.wit(mic: 'start')
    spop: (x) ->
      if x.query && x.total_tracks
        slice = (y) -> y.slice(0, 2)
        spop.cache = x.tracks.concat(slice(x.albums)).concat(slice(x.playlists))
        console.log(spop.cache)

      # p.hipchat(JSON.stringify(x))
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
        f(entities || {})
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
