![cherry](/docs/cherry.png)

# Cherry

An extensible hub for home automation/Internet of Things.

## Overview

Cherry acts as a hub for your house and allows any connected component to communicate with each other. Cherry's power comes from its plugin system. Connected devices talk to each other through the _firehose_, a message bus provided by Cherry. Adding a new component to the system is as simple as writing a few lines of code (Node.js module or ClojureScript namespace).

As an example, a Philips Hue plugin could wait for "to:lights" messages on the firehose and flip the lights in response. A GPIO plugin could send "from:pin" messages when a button is pressed on the Raspberry Pi. Finally, another plugin could read "from:pin" messages and turns them into "to:lights" messages. Boum, lights turn on and off when a button is pressed. This is easily expressed in code:

```javascript
module.exports = function (ec) {
  console.log("lightswitch ready to rock");

  ec.consume(function (msg) {
    console.log("lightswitch received", require('util').inspect(msg));
    switch (msg.from) {
      case "pin":
        if (msg.body === "low") {
          ec.produce({to: "lights", body: {on: true}});
        } else if (msg.body === "high") {
          ec.produce({to: "lights", body: {on: false}});
        }
        break;
    };
  });
}
```

## Using

```bash
npm install -g cherry-core
cherry path/to/config.json
```

`config.json` looks something like that (cf. config.json.sample):

```json
{
  "port": 4433,
  "witd_url": "http://192.168.1.68:8080",
  "wit_token": "MY_WIT_TOKEN",
  "hipchat_jid": "38888_1000000@chat.hipchat.com",
  "hipchat_pwd": "mypwd",
  "hipchat_room": "38888_myroom@conf.hipchat.com/Cherry",
  "hue_host": "http://192.168.1.169",
  "hue_user": "willyblandin",
  "mopidy_url": "ws://192.168.1.66:6680/mopidy/ws",
  "demo_port": 5576,
  "gpio_pins": {
    "22": ["in", "both"]
  },
  "plugins": [
    "./examples/lightswitch.js",
    "cherry-spotify",
    "cherry.integration.hipchat",
    "cherry.integration.wit",
    "cherry.integration.hue",
    "cherry.integration.gpio"
  ]
}
```

## Using existing plugins

In your `config.json` file, you specify the list of plugins you want to use.

Each item can either be:
- the name of a globally or locally installed npm package, e.g. `cherry-spotify`
- a path to a Javascript file, e.g. `./examples/lightswitch.js`
- a ClojureScript module (we're still figuring out how to allow cljs plugins)
- a CoffeeScript file (coming soon...)

## Creating a plugin

We've focused on making it really simple and easy to write a plugin for Cherry.
You can check the `examples` directory, [cherry-spotify](https://github.com/wit-ai/cherry-spotify) or below:

```bash
mkdir cherry-logger
npm init

cat > index.js <<EOF
module.exports = function (ec) {
  console.log("logging msg!");
  ec.consume(function(msg) {
    console.log("got", require('util').inspect(msg));
  });
}
EOF

npm publish
```

## Built-in plugins

You can configure plugins through a `config.json` file.

### Hue

Consumes: "to:lights"

```
"hue_host": "http://192.168.1.169"
```

### HipChat

Produces: "from:chat"

```
"hipchat_jid": "88888_8888888@chat.hipchat.com",
"hipchat_pwd": "mypassword",
"hipchat_room": "88888_yay@conf.hipchat.com/My Username",
```

### witd

Produces: "from:semantic"

```
"witd_url": "http://192.168.1.68:8080",
"wit_token": "MY_TOKEN",
```

### GPIO

Note: requires `sudo` to access pins on Raspberry Pi.

Produces: "from:pin"

```
"gpio_pins": {
  "22": ["in", "both"]
},
```

### Mopidy

Consumes: "to:music"
Produces: "from:music" with music info

```
"mopidy_url": "ws://192.168.1.66:6680/mopidy/ws"
```

## Dev

```bash
lein do cljsbuild clean, cljsbuild auto
cp config.json.sample config.json
node dist/cherry.js
```

## Cambridge

We use cherry everyday at the office and have put together a small script that should get everything up and running from a Raspberry Pi:

```bash
curl -s https://github.com/wit-ai/witd/raw/master/cambridge.sh | sudo -E sh
```

## TODO

- allow CoffeeScript plugins
- figure out how to allow CLJS plugins
