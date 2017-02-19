"use strict";

const path = require('path')
const fs = require('fs')
const async = require('async')
const nodemailer = require('nodemailer')
const addrs = require("email-addresses")
const mkdirp = require("mkdirp")
const winston = require('winston')
const _ = require('lodash')

const config = require('../../config.json')
const randomutil = require('../helpers/randomutil.js')
const User = require('../models/user.js')
const Frame = require('../models/frame.js')
const Listask = require('../models/listask.js')
const Holdedmail = require('../models/holdedmail.js')
const MultiaccWhitelist = require('../models/multiaccwhitelist.js')

const logger = new (winston.Logger)({
    transports: [ new (winston.transports.File)({ 
        filename: 'emailhandler.log',         
        json: false,
        timestamp: true 
    }) ]
});

function rebuildContent(data) {
    var result = `Received: by web11o.yandex.ru with HTTP;
    Fri, 18 Sep 2015 10:41:36 +0300
From: from <${data.from[0].address}>
To: to <${data.to[0].address}>
Subject: new photos
MIME-Version: 1.0
Message-Id: <${Date.now()}@web11o.yandex.ru>
X-Mailer: Yamail [ http://yandex.ru ] 5.0
Date: Fri, 18 Sep 2015 10:41:36 +0300
Content-Type: multipart/mixed;
    boundary="----==--bound.147221.web11o.yandex.ru"


------==--bound.147221.web11o.yandex.ru
Content-Transfer-Encoding: 8bit
Content-Type: text/html; charset=koi8-r

<div>photos</div>
------==--bound.147221.web11o.yandex.ru`

    for(var att of data.attachments) {
        result+=
`
Content-Disposition: attachment;
    filename="${att.fileName}"
Content-Transfer-Encoding: base64
Content-Type: ${att.contentType};
    name="${att.fileName}"

${att.content.toString('base64')}
------==--bound.147221.web11o.yandex.ru`
    }

    return result+"--"
}


function rebuildPasswd() {
    logger.info('rebuildPasswd started')
    Frame.find({}, function(err, frames) {
        var data = ""
        for(var frame of frames) {
            var pass = frame.password || config.defaultPassword
            data+= frame.name+":{PLAIN}"+pass+":1002:1003::/home/vmail/"+frame.name+"\n"
        }

        fs.writeFile('/etc/dovecot/passwd', data, function (err) {
            if (err) { throw err }
            logger.info('Passwd saved!');
        });
    })
}

function deliverLegacy(userFolder, content) {
    var randomString = randomutil.makeRandomString(16)
    var pathBase = path.join("/home/vmail/", userFolder, "Maildir")
    var pathNew = path.join(pathBase, "new", Date.now()+";"+userFolder+randomString)
    var pathTmp = path.join(pathBase, "tmp", Date.now()+";"+userFolder+randomString)
    logger.info("deliverLegacy started, saving to "+pathTmp)

    mkdirp(path.join(pathBase, "new"), {mode: 511}, function(err) {
        if(err) { throw err; }
        mkdirp(path.join(pathBase, "tmp"), {mode: 511}, function(err) {
            if(err) { throw err; }
            fs.writeFile(
                pathTmp, 
                content,
                function(err) {
                    if(err) { throw err; }
                    logger.info("Email writed")

                    fs.rename(pathTmp, pathNew, function(err) {
                        if(err) { throw err; }
                        logger.info("Email renamed")
                    })
                }
            )
        })
    })

}


function askUser(frameName, user, data, content) {
    logger.info("askUser start")
    var token = randomutil.makeToken()

    // send email
    var transporter = nodemailer.createTransport(config.emailTransport)

    var mailOptions = {
        from: config.emailFrom,
        to: user.email,
        subject: 'You Have A New Sender To Your Skylight - Approve or Block',
        text: 'Text text text ' + 'http://frame.uzere.name:1337/confirm/'+token + 
            ' ' + 'http://frame.uzere.name:1337/deny/'+token,
        html: `
        <p>Hello
        <p>${data.from[0].address} has sent a photo to one of your Skylights. 
        Do you wish to approve this sender for this and future photos? 
        Click one of the links below to indicate your decision.<br><br>

        <a href="http://${config.baseDomain}/confirm/${token}">Approve Sender</a><br><br>
        <a href="http://${config.baseDomain}/deny/${token}">Block Sender</a>
        `
    }

    transporter.sendMail(mailOptions, function(error, info){
        if(error){
            return logger.error(error)
        }
        logger.info('Message sent: ' + info.response)
    });


    Listask.create({user: user._id, emailToCheck: data.from[0].address, token: token}, function(err, data) {
        if(err) { throw err } // TODO
    })

    Holdedmail.create({from: data.from[0].address, toName: frameName, content: content}, function(err, data) {
        if(err) { throw err } // TODO
    })
}

function askAdmin(frames, users, data, content) {
    logger.info("askAdmin start")
    var token = randomutil.makeToken()

    // send email
    var transporter = nodemailer.createTransport(config.emailTransport)

    var mailOptions = {
        from: config.emailFrom,
        to: /*"gmail@uzere.name",*/"info@skylightframe.com",
        subject: 'Someone tries to email photos to Skylights representing more than one main account holder',
        text: 'Text text text ',
        html: `
        <p>Hello
        <p>Email is ${data.from[0].address} <br> 
        Click one of the links below to indicate your decision.<br><br>

        <a href="http://${config.baseDomain}/confirm/${token}">Approve Sender</a><br><br>
        <a href="http://${config.baseDomain}/deny/${token}">Block Sender</a>
        `,
        attachments: data.attachments.map(att =>({filename: att.fileName, content: att.content}))
    }

    transporter.sendMail(mailOptions, function(error, info){
        if(error){
            return logger.error(error)
        }
        logger.info('Message sent: ' + info.response)
    });


    Listask.create({emailToCheck: data.from[0].address, token: token, adminCheck: true}, function(err, data) {
        if(err) { throw err } // TODO
    })

    for(let frame of frames) {
        Holdedmail.create({from: data.from[0].address, toName: frame.name, content: content}, function(err, data) {
            if(err) { throw err } // TODO
        })
    }
}

function emailHandler(connection, data, content) {
    content = undefined // Try to save RAM

    logger.info("NEW MAIL TO", data.to)
    if(data.spamScore) {
        logger.info("SpamScore: ", data.spamScore)
    }
    if(data.cc && data.cc.length) {
        logger.info("CC", data.cc)
    }
    logger.info("FROM", data.from)

    for(var att of data.attachments) {
        logger.info(att.length+" "+att.checksum)
    }

    let efrom = data.from[0].address.toLowerCase()

    let frameNames = data.to
        .concat(data.cc || [])
        .map(rec => addrs.parseOneAddress(rec.address))
        .filter(parsedAddr => (parsedAddr.domain.toLowerCase()=="ourskylight.com" || parsedAddr.domain.toLowerCase()=="testmx.uzere.name"))
        .map(parsedAddr => parsedAddr.local.toLowerCase())

    let rebuildedContent = rebuildContent(data)

    ;(async function () {
        let frames = await Frame.find({name: {$in: frameNames}}).exec()
        let fids = frames.map(frame => frame._id)
        let users = await User.find({frames: {$in: fids}}).exec()

        let framesById = _.keyBy(frames, '_id')

        if(users.length == 0) {
            logger.info("No frames found")
            return
        } 

        if(users.length > 1) {
            let allowed = await MultiaccWhitelist.findOne({email: efrom}).exec()
            if(!allowed) {
                askAdmin(frames, users, data, rebuildedContent)
                return
            }
        }

        for(let user of users) {
            let w = user.whitelist
            let b = user.blacklist
            logger.info("For user lists: ", [w, b])
                
            for(let frameId of user.frames) {
                if(framesById[frameId] != null) {
                    let frameName = framesById[frameId].name
                    logger.info("Frame name ", frameName)

                    if((!w.length || w.indexOf(efrom)>-1) && b.indexOf(efrom)==-1) {
                        logger.info("Delivered from ", efrom)
                        deliverLegacy(frameName, rebuildedContent)
                    } else if (w.length && w.indexOf(efrom)==-1 && b.indexOf(efrom)==-1) {
                        logger.info("Holded from ", efrom)
                        askUser(frameName, user, data, rebuildedContent)
                    } else if (b.indexOf(efrom)>=-1) {
                        logger.info("Dropped from ", efrom)
                    }
                }
            }
        }

    })(
    ).catch(e => { console.log(e); throw e })

    // for(var frameName of frameNames) {
    //     Frame.findOne({name: frameName}, function(err, frame) {
    //         if(err) { /*throw err;*/ return; /*do nothing*/ } // TODO
    //         if(frame===null) { logger.info("No frames found for ", frameName); return; }

    //         User.findOne({frames:   {$elemMatch:{$gte:frame._id, $lte: frame._id}}    }, function(err, user) {
    //             if(err) { throw err; } // TODO
    //             var w = user.whitelist
    //             var b = user.blacklist
    //             logger.info("Frame name ", frameName)
    //             logger.info("For user lists: ", [w, b])
    //             var efrom = data.from[0].address.toLowerCase()

    //             if((!w.length || w.indexOf(efrom)>-1) && b.indexOf(efrom)==-1) {
    //                 logger.info("Delivered from ", efrom)
    //                 deliverLegacy(frameName, rebuildedContent)
    //             } else if (w.length && w.indexOf(efrom)==-1 && b.indexOf(efrom)==-1) {
    //                 logger.info("Holded from ", efrom)
    //                 askUser(frameName, user, data, rebuildedContent)
    //             } else if (b.indexOf(efrom)>=-1) {
    //                 logger.info("Dropped from ", efrom)
    //             }

    //         })
    //     })
    // }


}


module.exports = {
    handler: emailHandler,
    deliverLegacy,
    rebuildPasswd
}


