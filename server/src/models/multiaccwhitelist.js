"use strict";

const mongoose = require('mongoose')

let schema = new mongoose.Schema({
	email: String
})

let MultiaccWhitelist = mongoose.model('MultiaccWhitelist', schema)

module.exports = MultiaccWhitelist