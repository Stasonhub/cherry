module.exports = function (cherry) {
  console.log("lightswitch ready to rock");

  cherry.handle({
    pin: function (message) {
      if (msg.body === "high") {
        cherry.hue({on: true});
      } else if (msg.body === "low") {
        cherry.hue({on: false});
      }
    }
  });
}
