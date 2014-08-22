# Cherry

An extensible hub for home automation/Internet of Things.

## Overview

Cherry acts as a hub for your house and allows any connected component to communicate with each other. Cherry's power comes from its plugin system. Connected devices talk to each other through the _firehose_, a message bus provided by Cherry. Adding a new component to the system is as simple as writing a few lines of code (Node.js module or ClojureScript namespace).

As an example, a Philips Hue plugin could wait for "to:lights" messages on the firehose and flip the lights in response. A GPIO plugin could send "from:pin" messages when a button is pressed on the Raspberry Pi. Finally, another plugin could read "from:pin" messages and turns them into "to:lights" messages. Boum, lights turn on and off when a button is pressed.

Cherry also acts as a repository for home automation/IoT plugins. We provide a few plugins out-of-the-box (Hue, HipChat, Wit, Mopidy, GPIO, etc.) and will accept pull requests for other systems.

## Using

```bash
lein do cljsbuild clean, cljsbuild once
cp config.json.sample config.json
node dist/cherry.js
```

## Building

```bash
lein do cljsbuild clean, cljsbuild once
node dist/cherry.js
```

## Creating a plugin

We've focused on making it really simple and easy to write a plugin for Cherry.
cf. `examples` directory

## Built-in plugins

You can configure plugins through a `config.json` file.

### Mopidy

Consumes: "to:music"
Produces: "from:music" with music info

```
"mopidy_url": "ws://192.168.1.66:6680/mopidy/ws"
```

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
