"use strict";

module.exports = {
    catch404: function(req, res, next) {
        var err = new Error('Not Found')
        err.status = 404
        next(err)
    },

    handler: function(err, req, res, next) {
        res.status(err.status || 500)
        res.render('error', {
            message: err.message,
            error: req.app.get('env') === 'development' ? err : {}  
        })
    },

    /**
     * Event listener for HTTP server "error" event.
     */
    onServerError: function(error) {
        if (error.syscall !== 'listen') {
            throw error
        }

        var bind = 'Port'

        // handle specific listen errors with friendly messages
        switch (error.code) {
            case 'EACCES':
                console.error(bind + ' requires elevated privileges')
                process.exit(1)
                break
            case 'EADDRINUSE':
                console.error(bind + ' is already in use')
                process.exit(1)
                break
            default:
                throw error
        }
    }
}   