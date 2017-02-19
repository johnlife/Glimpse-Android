"use strict";

const mongoose = require('mongoose')

let schema = new mongoose.Schema({
	toName: String, //login, without @...
	from: String,
	content: String
})

let Holdedmail = mongoose.model('Holdedmail', schema)

module.exports = Holdedmail