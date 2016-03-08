MusicSPA-AngularJS-NoAuth
=========================


Table of Contents
-----------------

01. What is the "MusicSPA"?
02. Prerequisites to build, test and run the project
03. How to run the app (from the command line)
04. How to view/edit the project source code
05. Why AngularJS?


01. What is the "MusicSPA"?
---------------------------

This simple App is a "Single Page Application (SPA)". It is
- a web application running in the browser
- using web frontend technologies HTML, CSS, JavaScript
- with web pages being rendered in the browser
- using AngularJS as client-side MVC framework
- and Bootstrap web style sheets.

The MusicSPA manages music Performers and Recordings. It is a REST client
to the web service MusicService which hosts the data.
The MusicSPA cannot do any work without MusicService being started.

With MusicSPA the user can perform the following operations in the browser:
- Add, change, delete and query Performers and Recordings
- listen to Recordings

The App is a small case study using ...
- AngularJS as client side Web framework
- JavaScript as implementation language
- The ngResource library to access the REST API of the MusicService
- Bootstrap CSS to enhance the look of the web pages
- Currently no client authentication to access performers or recordings
- Currently no client roles with different access priviledges

In the current stage the App doesn't implement any security features.
It communicates with the MusicService via HTTP (no HTTPS) and does not require
the user to log in before accessing the services data.

The project build and management of components is based on npm, bower and gulp.


02. Prerequisites to build, test and run the project
----------------------------------------------------

Installation of

- NodeJS
- bower
- gulp
- jshint

If not yet installed, download and install [NodeJS](https://nodejs.org) on your system.
This also makes npm (Node package manager) available on your machine.

With npm being available, use it to install the other tools on your system:
```bash
    $ npm install bower -g
    $ npm install gulp -g
    $ npm install jshint -g
```
If working on OSX or Linux precede these commands with "sudo".


03. How to run the app (from the command line)
----------------------------------------------

In order to run the app MusicService must already be running at http://localhost:9000.
(This URL can be changed in app/scripts/services.js.)

After having lauched MusicService go to the MusicSPA project directory and run this command:
```bash
    $ gulp watch
```
This command builds and packages MusicSPA from the source files to the dist dirctory,
deploys the app into a local server and opens a browser window in which it presents
the start page of the app.


04. How to view/edit the project source code
--------------------------------------------

Use any text editor to view or edit the code.
"Brackets" is a very good choice for editing HTML, CSS and JavaScript.

See/download:

- http://brackets.io


05. Why AngularJS?
------------------

There are other frameworks out there (Ember, Backbone, React, Meteor, Polymer, Knockout etc.).
I chose Angular as it is currently the leading client-side MVC web framework in the market.
