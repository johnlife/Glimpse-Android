"use strict";

var mongoose = require('mongoose');

module.exports = {
	connect: function(cb) {
		mongoose.connect('mongodb://localhost/skylight')
		var db = mongoose.connection
		db.on('error', console.error.bind(console, 'connection error:'))
		db.once('open', function () {
			console.log("DB connection open")
			cb()
		})

	}
}