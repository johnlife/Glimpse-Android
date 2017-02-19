"use strict";

const mongoose = require('mongoose')

let schema = new mongoose.Schema({
	name: String,
	password: { type: String, default: "" },
	code: { type: String, default: "" },
	isLegacy: Boolean,
	resetNeeded: { type: Boolean, default: false },

	orderNumber: String,
	createdAt: Date
})

let Frame = mongoose.model('Frame', schema)

module.exports = Frame