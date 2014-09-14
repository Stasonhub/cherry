module.exports = function (cherry) {
  console.log("lightswitch ready to rock");

  cherry.handle({
    pin: function (message) {
      if (message.body === "high") {
        cherry.hue({on: true});
      } else if (message.body === "low") {
        cherry.hue({on: false});
      }
    }
  });
}
