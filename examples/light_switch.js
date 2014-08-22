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
