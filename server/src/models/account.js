"use strict";

const mongoose = require('mongoose')
const passportLocalMongoose = require('passport-local-mongoose')

let Account = new mongoose.Schema({
    username: String,
    password: String
});

Account.plugin(passportLocalMongoose)

module.exports = mongoose.model('Account', Account)