module.exports = function (ec) {
  var util = require('util');
  ec.consume(function (x) {
    console.log("sniffed", util.inspect(x));
  });
}