"use strict";

const mongoose = require('mongoose')

let schema = new mongoose.Schema({
    email: { type: String, index: true }, 
    whitelist: [String],
    blacklist: [String],
    frames: { type: [mongoose.Schema.Types.ObjectId], index: true },
    updatedAt: { type: Date, index: true }
})

let User = mongoose.model('User', schema)

module.exports = User