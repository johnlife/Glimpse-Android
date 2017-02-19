"use strict";

const csv = require('csv')
const express = require('express')
const validator = require('validator')
const busboy = require('connect-busboy')
const nodemailer = require('nodemailer')
const async = require('async')
const _ = require('lodash')

const randomutil = require('../helpers/randomutil.js')
const emailhandler = require('../helpers/emailhandler.js')
const config = require('../../config.json')
const User = require('../models/user.js')
const Frame = require('../models/frame.js')

const router = express.Router();

const adminOnly = (req, res, next) => {
    if(!req.user || req.user.username!="admin") {
        return res.end("Please login as admin")
    } else {
        next()
    }
}

const RegExpEscape = function(s) {
    return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&')
}

/**
 * Takes a route handling function and returns
 * a function that wraps it in a `try/catch`. Caught
 * exceptions are forwarded to the `next` handler.
 */
function A(routeHandler) {
  return async function (req, res, next) {
    try {
      await routeHandler(req, res, next);
    } catch (err) {
      console.log(err)
      next(err);
    }
  }
}



//TODO validate body email name
router.param('email', function (req, res, next, email) {
    if(!validator.isEmail(email)) {
        return next(new Error("Invalid :email"))
    }
    req.email = email
    return next()
})

router.param('id', function (req, res, next, id) {
    if(!validator.isMongoId(id)) {
        return next(new Error("Invalid :id"))
    }
    req.id = id
    return next()
})

router.param('msgId', function (req, res, next, msgId) {
    // TODO
    req.msgId = msgId
    return next()
})

router.param('code', function (req, res, next, code) {
    if(!validator.isLength(code, config.frameCodeLength, config.frameCodeLength)) {
        return next(new Error("Invalid :code length"))
    }    
    if(!validator.isNumeric(code)) {
        return next(new Error("Invalid :code, should contain digits only"))
    }
    req.code = code
    return next()
})



router.get('/users', adminOnly, function(req, res, next) {
    User.find({ }).sort({updatedAt: 1}).exec( function(err, users) {
        if (err) { return next(err) }
        res.json(users)
    })
})

router.get('/user/:email', function(req, res, next) {
    User.findOne({email: req.email}, function(err, user) {
        if (err) { return next(err) }
        res.json(user)
    })
})

router.post('/user/:email', function(req, res, next) {
    let emailRE = "^"+RegExpEscape(req.email)+"$"
    User.findOne({email: { $regex: emailRE, $options: "i" }}, function(err, user) {
        if (err) { return next(err) }
        if(user) {
            return next(new Error("User already exists"))
        }
        User.create({email:req.email, checklist: [], frames: []}, function(err, user) {
            if (err) { return next(err) }
            res.json(user)
        })
    })

})

router.delete('/user/:email', function(req, res, next) {
    User.remove({email:req.email}, function(err, user) {
        if (err) { return next(err) }
        res.json({})
    })
})



// manage lists
router.post('/user/:email/whitelist', function(req, res, next) {
    User.update({email:req.email}, {$push: {whitelist: req.body.email}}, function(err, user) {
        if (err) { return next(err) }
        res.json({})
    })
})

router.post('/user/:email/blacklist', function(req, res, next) {
    User.update({email:req.email}, {$push: {blacklist: req.body.email}}, function(err, user) {
        if (err) { return next(err) }
        res.json({})
    })
})

router.delete('/user/:email/whitelist', function(req, res, next) {
    User.update({email:req.email}, {$pull: {whitelist: req.body.email}}, function(err, user) {
        if (err) { return next(err) }
        res.json({})
    })
})

router.delete('/user/:email/blacklist', function(req, res, next) {
    User.update({email:req.email}, {$pull: {blacklist: req.body.email}}, function(err, user) {
        if (err) { return next(err) }
        res.json({})
    })
})



// get frames
router.get('/user/:email/frames', function(req, res, next) {
    if(!req.user || req.user.username!="admin") {
        return next("Please login as admin")
    }

    User.findOne({email: req.email}, function(err, user) {
        if (err) { return next(err) }
        Frame.find({_id: {$in: user.frames}}, function(err, frames) {
            if (err) { return next(err) }
            res.json(frames)
        })
    })
})

// add frame
router.post('/user/:email/frames', function(req, res, next) {
    // TODO check unique
    if(!/^[a-zA-Z0-9\.\-_]*$/.test(req.body.name)) {
        return next("Wrong frame name")
    }

    Frame.create({  name: req.body.name, isLegacy: true, code: randomutil.makeCode()}, function(err, frame) {
        if (err) { return next(err) }
        User.update({email: req.email}, {$push: {frames: frame._id}}, function(err, user) {
            if (err) { return next(err) }
            emailhandler.rebuildPasswd()
            res.json(frame)
        })
    })
})

// delete frame
router.delete('/frame/:id', adminOnly, A(async function(req, res, next) {
    let frame = await Frame.remove({_id: req.id}).exec()
    let user = await User.update({frames: req.id}, {$pull: {frames: req.id}})

    emailhandler.rebuildPasswd()
    res.json({})
}))

// new code 
router.post('/frame/:id/newcode', adminOnly, A(async function(req, res, next) {
    do {
        var code = randomutil.makeCode()
        let frame = await Frame.find({code}).exec()
    } while(frame)

    let frame = Frame.update({_id: req.id}, {$set: {code} }).exec()
    res.json({})
}))

router.get('/bind/:code', A(async function(req, res, next) {
    let frame = await Frame.findOne({code: req.code}).exec()

    if (!frame) {
        return res.json({error: "Code is used | Wrong code"})
    }

    if (req.query.noDelete!="true") {
        frame.code = null
    }

    if (req.query.newPassword=="true") {
        frame.password = randomutil.makePassword()
    }

    frame = await frame.save()
    res.json({name: frame.name, password: frame.password})

    if (req.query.newPassword=="true") {
        emailhandler.rebuildPasswd()
    }
}))

// get messages (new photo)
router.get('/frame/:id/messages', function(req, res, next) {
    res.json({})
})

// delete readed messages
router.delete('/frame/:id/messages/:msgId', function(req, res, next) {

})



router.use(busboy())
router.post('/like', function(req, res, next) {
    var photoFile = new Buffer([]), sendTo
    if (req.busboy) {
        req.busboy.on('file', function(fieldname, file, filename, encoding, mimetype) {
            file.on('data', function(data) {
                //console.log(data)
                photoFile = Buffer.concat([photoFile, data])
            })
            file.on('end', function() {
            })
        })

        req.busboy.on('field', function(key, value, keyTruncated, valueTruncated) {
            if(key=='email'){
                sendTo = value
            }
        })

        req.busboy.on('finish', function() {
            var transporter = nodemailer.createTransport(config.emailTransport)

            var mailOptions = {
                from: config.emailFrom,
                to: sendTo,
                subject: 'Someone liked a photo you sent to Skylight',
                text: 'Guess what? Your loved one wanted to thank you for the attached photo you sent to their Skylight - they loved it!\n\n'+
                    'Keep on sending new photos to their Skylight -- they\'re checking it, and you might just make their day!\n\n'+
                    '-Team Skylight',
                attachments: [{
                    filename: 'photo.jpg',
                    content: photoFile
                }]
            }

            transporter.sendMail(mailOptions, function(error, info){
                
            });

            res.json({})
            res.end();
        });

        req.pipe(req.busboy);
    }
})



router.get('/export', adminOnly, A(async function(req, res, next) {
    let input = []

    let frames = _.keyBy(await Frame.find().exec(), '_id')

    let users = await User.find().exec()

    for(let user of users) {
        for(let frameId of user.frames) {
            input.push([
                frames[frameId].createdAt ? frames[frameId].createdAt.toISOString() : "",
                frames[frameId].orderNumber,
                frames[frameId].name,
                user.email,
                frames[frameId].code || "-"
            ])
        }
    }

    csv.stringify(input, function(err, output){
        res.attachment((new Date()).toDateString()+'.csv');
        res.end(output)
    })
}))

function getFile(req) {
    return new Promise(function(resolve, reject) {
        if (!req.busboy) {
            reject('no file')
        }

        var data = new Buffer([])


        req.busboy.on('file', function(fieldname, file, filename, encoding, mimetype) {
            file.on('data', function(newData) {
                data = Buffer.concat([data, newData])
            })
            file.on('end', function() { })
        })

        req.busboy.on('finish', function() {
            resolve(data)
        })

        req.pipe(req.busboy);
    })
}

router.post('/import', adminOnly, A(async function(req, res, next) {
    let csvData = await getFile(req)
    let epoch = Date.parse("1970-01-01T00:00:00.000Z")
    
    csv.parse(csvData.toString(), async function(err, data) { try {
        if (err) { return next(err) }

        var framesIgnored = 0, framesAdded = 0, usersCreated = 0, framesIgnoredArr = []

        for(let row of data) {
            if(row.length==5) {
                var [time, orderNumber, name, userEmail, code] = row
            } else if(row.length==3) {
                var [name, userEmail, code] = row
            } else {
                return next('incorrect file format')
            }

            name = name.toLowerCase()

            let frame = await Frame.findOne({name: name}).exec()
            if(frame) {
                framesIgnoredArr.push(name+', user: '+userEmail)
                framesIgnored++
                continue;
            }

            code = code || randomutil.makeCode()
            code = code=="-" ? "" : code
            time = new Date(time)

            if(row.length==5) {
                frame = await Frame.create({name, isLegacy: true, code, orderNumber})
                if(time > epoch) {
                    frame.createdAt = time
                }
            } else {
                frame = await Frame.create({name, isLegacy: true, code})
            }
            framesAdded++

            let emailRE = "^"+RegExpEscape(userEmail)+"$"
            let user = await User.findOneAndUpdate({email: { $regex: emailRE, $options: "i" }}, {$push: {frames: frame._id}}).exec()

            if(!user) { //create user
                await User.create({email: userEmail, frames: [frame._id]})
                usersCreated++
            }
        }

        emailhandler.rebuildPasswd()
        res.render('imported',{
            framesIgnored,
            framesAdded,
            usersCreated,
            framesIgnoredArr
        })

    } catch(e) {
        next(e)
    }})
}))



router.post('/resetStatus', A(async function(req, res, next) {
    let frame = await Frame.findOne({name: req.body.name}).exec()

    var resetStatus = false
    if(frame) {
        resetStatus = frame.resetNeeded
    }

    res.json({reset: resetStatus})

    if(frame && resetStatus) {
        let id = frame._id
        let deletedFrame = await Frame.remove({_id: id}).exec()
        let user = await User.update({frames: id}, {$pull: {frames: id}}).exec()
        emailhandler.rebuildPasswd()
    }
}))

router.post('/setResetStatus', adminOnly, A(async function(req, res, next) {
    let frame = await Frame.findOne({name: req.body.name}).exec()

    if(!frame) { return res.json({error: "Frame not found"})}

    frame.resetNeeded = !frame.resetNeeded
    await frame.save()
    
    res.json({})
}))


var remindInProgress = false
router.post('/remind',/* adminOnly,*/ A(async function(req, res, next) {
    if(remindInProgress) {
        res.json({started: false})
        return
    }
    remindInProgress = true
    res.json({started: true})

    var emailToArray = ['gmail@uzere.name', 'z-zon@ya.ru'] /////////////////////////////

    var transporter = nodemailer.createTransport(config.emailTransport)


    async.eachSeries(emailToArray, function (emailTo, done) {
        setTimeout(function () {
            User.findOne({email: emailTo}, function(err, user) {
                if (err) { throw err }
                Frame.find({_id: {$in: user.frames}}, function(err, frames) {
                    if (err) { throw err }
                    //res.json(frames)
                    var framesList = frames.map(f => f.name).map(s => s+'@ourskylight.com')

                    var frames = framesList.join(', ')

                    var mailOptions = {
                        from: config.emailFrom,
                        to: emailTo,
                        replyTo: framesList,
                        subject: 'Send your loved one a photo! Reply to this email to send to their Skylight.',
                        text: 'Hey there Skylighter!\n\n'+
                            'This email is the easiest way for you to send a treasured photo to your loved one\'s Skylight.\n\n'+
                            'Just hit "reply", attach a photo, and send! Your photo will pop up instantly on the following Skylight: '+frames+'\n\n'+
                            'Happy sharing!\n\n'+
                            'Team Skylight\n\n'
                    }

                    console.log('sending', emailTo, frames)
                    transporter.sendMail(mailOptions, function(error, info){
                        done();
                    });
                })
            })
        }, 1000);
    }, function (err) {
        //if (!err) callback();
        remindInProgress = false
    });
}))





module.exports = router;
