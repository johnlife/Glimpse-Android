"use strict";

const http = require('http')
const express = require('express')
const path = require('path')
const favicon = require('serve-favicon')
const cookieParser = require('cookie-parser')
const bodyParser = require('body-parser')
const mailin = require('mailin')
const fs = require('fs')
const async = require('async')
const passport = require('passport')
const LocalStrategy = require('passport-local').Strategy
const winston = require('winston')

const error = require('./helpers/error.js')
const db = require('./helpers/db.js')
const emailhandler = require('./helpers/emailhandler.js')

const routes = require('./routes/index')
const api = require('./routes/api')

//
const process = require('process')
process.umask(0)
//

let unless = function(path, middleware) {
    return function(req, res, next) {
        if (path.indexOf(req.path)>-1) {
            return next();
        } else {
            return middleware(req, res, next);
        }
    };
};

// disable mailin console log
var mailinLogger = require('mailin/lib/logger')
setTimeout( () => mailinLogger.remove(winston.transports.Console), 2000)

var app = express()

app.set('views', path.join(__dirname, '../views'))
app.set('view engine', 'jade')

app.use(favicon(path.join(__dirname, '../public', 'favicon.ico')))
app.use(unless(["/api/v1/like","/api/v1/import"], bodyParser.json()))
app.use(unless(["/api/v1/like","/api/v1/import"], bodyParser.urlencoded({ extended: false })))
app.use(cookieParser())
app.use(require('express-session')({
    secret: 'k.ghjSFGHDFSGJntubs;r5p68hs945d;rotsurlt',
    resave: false,
    saveUninitialized: false
}))
app.use(passport.initialize());
app.use(passport.session());
app.use(express.static(path.join(__dirname, '../.tmp/public')))
app.use(express.static(path.join(__dirname, '../public'))) // FIXME
app.use('/uploads', express.static(path.join(__dirname, '../data'))) 

var Account = require('./models/account');
passport.use(new LocalStrategy(Account.authenticate()));
passport.serializeUser(Account.serializeUser());
passport.deserializeUser(Account.deserializeUser());

/* Account.register(new Account({ username : "admin" }), "bih7drptby", function(err, account) {
console.log(arguments)
    });//*/


// use routes
app.use('/', routes)
app.use('/api/v1', api)

// catch 404 and forward to error handler
app.use(error.catch404)
app.use(error.handler)


mailin.start({
    port: 25,
    disableWebhook: true,
    logFile: 'mailin.log',
    smtpOptions: { 
        disableDNSValidation: true,
        disableDnsValidation: true,
        disableDkim: true, 
        disableSpf: true
    }
});

mailin.on('message', emailhandler.handler);


async.series([
    db.connect,

    function() {
        app.set('port', 1337)

        var server = http.createServer(app)

        server.listen(app.get('port'))
        server.on('error', error.onServerError)
        server.on('listening', function() {
            console.log('Listening on ' + app.get('port'))
        })
    }

], function serverStartError(err, results) {
    console.log('serverStartError', err)
})
