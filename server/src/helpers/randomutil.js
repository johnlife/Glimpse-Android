"use strict";

const config = require('../../config.json')

function makeRandomString(length) {
	let text = ""
	let possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

	for(let i=0; i < length; i++) {
		text += possible.charAt(Math.floor(Math.random() * possible.length))
	}

	return text
}


module.exports = {
	makeToken: function() {
		return makeRandomString(config.tokenLength)
	},

	makePassword: function() {
		return makeRandomString(config.framePasswordLength)
	}, 

	makeCode: function() {
		let text = ""
		let possible = "0123456789"

		for( let i=0; i < config.frameCodeLength; i++ ) {
			text += possible.charAt(Math.floor(Math.random() * possible.length))
		}

		return text;
	},

	makeRandomString
}