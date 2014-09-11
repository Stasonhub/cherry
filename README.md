![cherry](/docs/cherry.png)

# Cherry

An extensible hub for home automation/Internet of Things.

## Overview

Cherry acts as a hub for your house and allows any connected component to communicate with each other. Cherry's power comes from its plugin system. Connected devices can talk to each other. Adding a new component to the system is as simple as writing a few lines of code (Node.js module).

As an example, let's say you have a Philips Hue light and you want to turn it on by pressing a button. You just need a few lines of code:

```javascript
module.exports = function (cherry) {
  console.log("lightswitch ready to rock");

  cherry.handle({
    pin: function (message) {
      var plugins = cherry.plugins();
      if (message.state === "high") {
        plugins.hue({on: true});
      } else if (message.state === "low") {
        plugins.hue({on: false});
      }
    }
  });
}
```

## Using

```bash
npm install -g cherry-core
# you may install additional plugins through npm
# npm install -g cherry-wit cherry-spotify cherry-hue
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
  "demo_port": 5576,
  "gpio_pins": {
    "22": ["in", "both"]
  },
  "plugins": [
    "cherry-spotify",
    "cherry-hue",
    "cherry-wit",
    "cherry-gpio",
    "cherry.integration.hipchat",
    "./contrib/cambridge.js",
  ]
}
```

## Using existing plugins

In your `config.json` file, you specify the list of plugins you want to use.

Each item can either be:
- the name of a globally or locally installed npm package, e.g. `cherry-spotify`
- a path to a Javascript file, e.g. `./examples/lightswitch.js`
- a CoffeeScript file

## Creating a plugin

We've focused on making it really simple and easy to write a plugin for Cherry.
You can check the `examples` directory, [cherry-spotify](https://github.com/wit-ai/cherry-spotify), [cherry-hue](https://github.com/wit-ai/cherry-hue) or below:

```bash
mkdir cherry-logger
npm init

cat > index.js <<EOF
module.exports = function (cherry) {
  // listen for chat messages and turn lights on or play next song
  cherry.handle({
    chat: function (msg) {
      var plugins = cherry.plugins();

      if (msg === 'next song') {
        plugins.spop('next');
      } else if (msg === 'lights on') {
        plugins.hue({on: true});
      }
    }
  });
}
EOF

npm publish
```

## Built-in plugins

You can configure plugins through a `config.json` file.

### HipChat

Note: this will be extracted into a `cherry-hipchat` plugin pretty soon.
Produces: "from: chat"

```
"hipchat_jid": "88888_8888888@chat.hipchat.com",
"hipchat_pwd": "mypassword",
"hipchat_room": "88888_yay@conf.hipchat.com/My Username",
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
curl -s https://raw.githubusercontent.com/wit-ai/cherry/master/cambridge.sh | sudo -E sh
```

## TODO

- figure out how to allow CLJS plugins
