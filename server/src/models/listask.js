"use strict";

const mongoose = require('mongoose')

let schema = new mongoose.Schema({
	user: mongoose.Schema.Types.ObjectId,
	emailToCheck: String,
	token: String,
	adminCheck: { type: Boolean, default: false }
})

let Listask = mongoose.model('Listask', schema)

module.exports = Listask