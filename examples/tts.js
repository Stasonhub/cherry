var net = require('net');

/*
Hack to forward text to a Mac and get back audio output.

Setup is as follow (Pi is .68, Mac is .246):
- on the Mac, we listen for text message, and send back audio using TCP
  - for i in $(seq 1 10000); do nc -l 0.0.0.0 1334 | xargs say -o say_out; done
  - fswatch -o say_out.aiff | xargs -n1 -I{} ./saycat.sh 192.168.1.68 1337
  - with saycat.sh containing:
    #!/bin/bash

    echo "forwarding voice to $1:$2"
    cat say.aiff | sox - -r 16000 -b 16 -c 1 -e signed-integer -t wavpcm - | nc $1 $2
- on the Raspberry Pi (requires sox to play back)
  - for i in $(seq 1 1000); do nc -l 0.0.0.0 1337 | play -r 16000 -b 16 -c 1 -e signed-integer -t wavpcm - ; done

- in cherry's config.json
  "tts": { "host": "192.168.1.246", "port": 1334 }
*/

module.exports = function (ec) {
  ec.consume(function (msg) {
    if (msg.to !== 'tts') {
      return;
    }

    console.log("[tts] got", msg);

    var socket;
    socket = new net.Socket();

    socket.on('error', function (err) {
      console.log("[tts] error from socket", err);
    });

    socket.on('connect', function () {
      console.log('[tts] socket connected');
      socket.write(msg.body);
      socket.end();
    });

    socket.on('close', function () {
      console.log('[tts] socket closed');
      socket.destroy();
    });

    socket.connect(ec.config.tts.port, ec.config.tts.host);
  });
}