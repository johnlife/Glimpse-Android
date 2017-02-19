"use strict";

var express = require('express')
var router = express.Router()
var passport = require('passport')
const nodemailer = require('nodemailer')
var pdf = require('phantom-html2pdf');
var winston = require('winston')
var async = require('async')
var markdownpdf = require("markdown-pdf")

var emailhandler = require('../helpers/emailhandler.js')
const MultiaccWhitelist = require('../models/multiaccwhitelist.js')
var Listask = require('../models/listask.js')
var Holdedmail = require('../models/holdedmail.js')
var User = require('../models/user.js')
var Frame = require('../models/frame.js')
const config = require('../../config.json')
var randomutil = require('../helpers/randomutil.js')

Object.values = obj => Object.keys(obj).map(key => obj[key]);

const RegExpEscape = function(s) {
    return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')
}

var logger = new (winston.Logger)({
    transports: [ new (winston.transports.File)({ 
        filename: 'emailhandler.log',         
        json: false,
        timestamp: true 
    }) ]
});


router.param('token', function (req, res, next, token) {
    // TODO
    req.token = token;
    return next()
})

router.get('/', function(req, res, next) {
    res.redirect('http://skylightframe.com')
})

router.get('/finalstep', function(req, res, next) {
    let email = req.query.email
    let number = req.query.number
    let errors = JSON.parse(req.query.errors || "{}")
    let emails = decodeURIComponent(req.query.emails) || "''"
    let noEmailInUrl = !req.query.email || req.query.noEmailInUrl

    res.render('confirmation', {noEmailInUrl, email, number, errors, emails, pageTitle: 'Skylight'})
})

function createFrames(user, emails, orderNumber, callback) {
    var ids = []
    var newFrames = []

    var now = new Date()

    async.eachSeries(emails, function(row, cb) {
        Frame.create({  name: row, isLegacy: true, code: randomutil.makeCode(), createdAt: now, orderNumber}, function(err, frame) {
            if (err) { return cb(err) }
            ids.push(frame._id)
            newFrames.push(frame)
            cb()
        })
    }, function(err) {
        if (err) { return callback(err) }

        let userFrames = user.toObject().frames

        user.frames =  userFrames.concat(ids)

        user.updatedAt = now

        user.save(function(err) {
            if (err) { return callback(err) }
            emailhandler.rebuildPasswd()
            callback(null, newFrames)
        })
    })
}

router.post('/orderconfirmed', async function(req, res, next) {
    let email = req.body.email || req.body.mainEmail
    let number = req.body.number

    let emails = {}
    let errors = {}

    for(var param of Object.keys(req.body)) {
        let matches = param.match(/^inputEmail(\d+)/)
        if(matches && matches.length == 2 && req.body[param]!="") {
            emails[matches[1]] = req.body[param].toLowerCase()

            if(!/^[a-zA-Z0-9\.\-_]*$/.test(req.body[param])) {
                errors[matches[1]] = `Email ${ req.body[param] } is invalid.`
            }
        }
    }

    Frame.find({name: {$in: Object.values(emails)}}, function(err, frames) {
        if (err) { return next(err) }

        if(frames.length > 0) {
            let names = frames.map(frame => frame.name+"@ourskylight.com").join(", ")
            errors[0] = `Email already exists: ${names}. Please select again.`
        }

        if(Object.keys(emails).length == 0) {
            errors[0] = `You didn't enter any emails`
        }

        if(Object.keys(errors).length > 0) {
            let er = encodeURIComponent(JSON.stringify(errors))
            let em = encodeURIComponent(JSON.stringify(emails))
            if(req.body.noEmailInUrl) {
                res.redirect(`/finalstep?email=${email}&number=${number}&errors=${er}&emails=${em}&noEmailInUrl=true`)
            } else {
                res.redirect(`/finalstep?email=${email}&number=${number}&errors=${er}&emails=${em}`)
            }
        } else {
            let emailRE = "^"+RegExpEscape(email)+"$"
            User.findOne({email: { $regex: emailRE, $options: "i" }}, async function(err, user) {
                if (err) { return next(err) }

                if(!user) {
                    user = await User.create({email, frames: []})
                }

                createFrames(user, emails, number, function(err, newFrames) {
                    if (err) { return next(err) }

                    let html = Object.values(newFrames).map(f=>`
<div style="font-family: Slabo, sans-serif; margin: 0 0 80px 0;">
    <div style="text-align: center; color: red; text-decoration: underline; margin: 10px auto; font-size: 10px;">DO NOT DISCARD. When Skylight prompts you, enter your one-time activation code below.</div>
    <div style="text-align: center; width: 100%; margin: 43px auto; font-size: 25px;">
        Your personal activation code for<br>
        Skylight email address<br>
        <b>${f.name}@ourskylight.com</b><br>
        is: <br><br>
        ${f.code}<br><br>
    </div>
    <div style="font-size: 8px;">Order ID: ${number}</div>
</div>
                    `).join("\n\n");

                    pdf.convert({html}, function(result) {
                        result.toFile("./.tmp/order.pdf", function() {
                            var transporter = nodemailer.createTransport(config.emailTransport)
                            var mailOptions = {
                                from: config.emailFrom,
                                to: "gmail@uzere.name",//*/'info@skylightframe.com',//info@skylightframe.com
                                subject: 'New @ourskylight.com email address is created', 
                                text: `User: ${email}\n\nFrames: ${ Object.values(emails).map(s=>s+"@ourskylight.com").join(", ") }.\n\n Order #${number}`,
                                attachments: [{
                                    filename: 'order.pdf',
                                    path: "./.tmp/order.pdf"
                                }]
                            }

                            transporter.sendMail(mailOptions, function(error, info){
                                if (err) { return next(err) }

                                if(req.body.noEmailInUrl) {
                                    res.render('orderconfirmed', {pageTitle: 'Skylight', noEmailInUrl: true, frames: newFrames.map(f=>({name: f.name, code: f.code}))})
                                } else {
                                    res.render('orderconfirmed', {pageTitle: 'Skylight', noEmailInUrl: false})
                                }
                            });
                        });
                    })


                })
            })

        }
        
    })

})

router.get('/admin', function(req, res, next) {
    let username = req.user ? req.user.username : ""
    
    res.render('index', { username })
});

router.get('/reset', function(req, res, next) {
    if(!req.user || req.user.username!="admin") {
        return res.end("Please login as admin")
    }

    Frame.find({resetNeeded: true}, function(err, frames) {
        if (err) { return next(err) }

        res.render('reset', {frames})
    })
})

router.get('/confirm/:token', function(req, res, next) {
    logger.info("CONFIRM page requested")
    Listask.findOneAndRemove({token: req.token}, function(err, data) {
        if (err) { return next(err) }
        if (!data) { return next(new Error("Invalid token")) }

        Holdedmail.find({from: data.emailToCheck}, function(err, mails) {
            if (err) { return next(err) }

            for(var mail of mails) {
                emailhandler.deliverLegacy(mail.toName, mail.content)
            }

            Holdedmail.remove({from: data.emailToCheck}, function(err, data2) {
                if (err) { return next(err) }
                
                if(!data.adminCheck) {
                    User.update({_id: data.user}, {$push: {whitelist: data.emailToCheck}}, function(err, user) {
                        if (err) { return next(err) }
                        res.render('confirm', {email: data.emailToCheck})
                        logger.info("CONFIRM page rendered")
                    })
                } else {
                    MultiaccWhitelist.create({email: data.emailToCheck}, function(err, obj) {
                        if (err) { return next(err) }
                        res.render('confirm', {email: "{{{"+data.emailToCheck+"}}} is now allowed to send emails to multiple users"})
                        logger.info("CONFIRM multiuser page rendered")
                    })
                }
            })

        })   
    })
})

router.get('/deny/:token', function(req, res, next) {
    logger.info("DENY page requested")
    Listask.findOneAndRemove({token: req.token}, function(err, data) {
        if (err) { return next(err) }
        if (!data) { return next(new Error("Invalid token")) }

        Holdedmail.remove({from: data.emailToCheck}, function(err, data2) {
            if (err) { return next(err) }

            User.update({_id: data.user}, {$push: {blacklist: data.emailToCheck}}, function(err, user) {
                if (err) { return next(err) }
                res.render('deny', {email: data.emailToCheck})
                logger.info("DENY page rendered")
            })
        })
    })
})

router.post('/login', function(req, res, next) {
  passport.authenticate('local', function(err, user, info) {
    //console.log(arguments)
    if (err) { return next(err) }
    if (!user) { return next(new Error("not auth1")) }
    req.login(user, function(err) {
        if (err) { return next(err) }
        return res.json({success: true})
    });
  })(req, res, next);
});

router.get('/logout', function(req, res, next) {
    req.logout();
    res.redirect('/admin');
})

module.exports = router;
