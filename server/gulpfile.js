"use strict";

const gulp = require('gulp')
const del = require('del')
const jshint = require('gulp-jshint')
const mkdirp = require('mkdirp')
const stylus = require('gulp-stylus')
const minifyCss = require('gulp-minify-css');
const babel = require('gulp-babel');

gulp.task('default', ['dev'])

gulp.task('dev', [/*'check',*/   'build'])

gulp.task('prod', ['build'])

// gulp.task('check', ['lint'])

gulp.task('build', ['stylus', 'scripts'])


gulp.task('scripts', ['initfs'], () =>
    gulp.src('./src/**/*.js')
        .pipe(babel({
            presets: [ 'stage-2', 'es2015'],
            plugins: ['transform-runtime', 'transform-async-to-generator']
        }))
        .on('error', function(e) {
            console.log('>>> ERROR', e);
            // emit here
            this.emit('end');
        })
        .pipe(gulp.dest('dist'))
);

gulp.task('clean', function (cb) {
    del([
        '.tmp/**/*',
        './dist/**/*'
    ], cb)
})

gulp.task('initfs', ['clean'], function(cb){
    mkdirp('.tmp/public/css', function(err) {
        if(err) { return cb(err) }
        mkdirp('.tmp/stylus', function(err) {
            if(err) { return cb(err) }
            cb()
        })
    })
})

// gulp.task('lint', function() {
//     return gulp.src(['./src/**/*.js', 'gulpfile.js'])
//         .pipe(jshint())
//         .pipe(jshint.reporter('default'))
// })

gulp.task('stylus', ['initfs'], function(){
    return gulp.src("./public/styles/*.styl")
        .pipe(stylus())
        .pipe(minifyCss())
        .pipe(gulp.dest('.tmp/public/css'))
})


gulp.task('watch', ['dev'], function() {
    gulp.watch('./src/**/*.js', ['scripts', 'stylus']);
    gulp.watch('./public/styles/*.styl', ['stylus']);
});