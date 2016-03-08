MusicMobileApp-Ionic-Cordova-NoAuth
===================================


Table of Contents
-----------------

01. What is the "MusicMobileApp"?
02. Prerequisites to build, test and run the project
03. How to run the app in the browser
04. How to run the app in an iOS emulator or on a real device
05. How to run the app in an Android emulator
06. How to run the app on a real Android device
07. How to view/edit the project source code
08. Why Ionic + Cordova?


01. What is the "MusicSPA"?
-------------------------------

This  App is a Hybrid Mobile Application. It is
- based on web technologies: HTML, CSS, JavaScript
- using Ionic, AngularJS and Cordova.

The MusicApp manages music Performers and Recordings. It is a RESTful
web service client to the web service MusicService.
The MusicApp cannot do any work without MusicService being started.

With MusicApp the user can perform the following operations on a mobile device:
- Add, change, delete and query Performers and Recordings
- listen to Recordings

The App is a small case study using ...
- the Ionic framework,
- which is based on AngularJS,
- Cordova to make the app available for Android and iOS,
- the ngResource library to access the REST API of the MusicService
- Currently no client authentication to access performers or recordings
- Currently no client roles with different access priviledges

In the current stage the App doesn't implement any security features.
It communicates with the MusicService via HTTP (no HTTPS) and does not require
the user to log in before accessing the services data.

The project build and management of components is based on ionic, npm, bower and gulp.


02. Prerequisites to build, test and run the project
----------------------------------------------------

Installation of

- NodeJS
- bower
- gulp
- jshint
- ionic
- cordova

If not yet installed, download and install [NodeJS](https://nodejs.org) on your system.
This also makes npm (Node package manager) available on your machine.

With npm being available, use it to install the other tools on your system:
```bash
    $ npm install bower -g
    $ npm install gulp -g
    $ npm install jshint -g
    $ npm install ionic -g
    $ npm install cordova -g
```
If working on OSX or Linux precede these commands with "sudo".


03. How to run the app in the browser
-------------------------------------

In order to run the app MusicService must already be running at http://localhost:9000.
(This URL can be changed in app/scripts/services.js. Alternatively start the App
and change the base URL in the app's settings.)

After having lauched MusicService go to the MusicMobileApp project directory and run this command:
```bash
    $ ionic serve
```
This command deploys the app into a local server and opens a browser window in which it presents
the start page of the app as a browser application.

Preferably run this command:
```bash
    $ ionic serve --lab
```
This command deploys the app into a local server and opens a browser window in which it presents
the start page of the app in the browser. The browser window presents the emulation of an iOS device
on the left and the emulation of an Android device on the right. That you way you can easily
compare the look an feel of the app under these two leading mobile platforms in the same browser window.

To access the data hosted in the MusicService this service mus be running.


04. How to run the app in an iOS emulator
-----------------------------------------

To run the app in an iOS emulator ...

- you must have a Mac (doesn't work on Windows or Linux)
- you must have XCode installed
- you need to install the node module _ios-sim_ (to let Ionic/Cordova control the iOS simulator)

If ios-sim is not yet installed, install on your Mac with:
```bash
    $ sudo npm install ios-sim -g
```
If these preconditions are met use
```bash
    $ ionic state reset
```
to install the configured platforms (ios and android) and cordova plugins.

Then invoke
```bash
    $ ionic resources
```
in order to generate icon and splash resources for different sizes in ios and Android.

Before starting the emulator find the IP address of your computer where MusicService is running.
In www/js/services.js (line 6) change the "defaultURL" accordingly. Do not use "localhost"
or "127.0.0.1", use the external IP address of the host MusicService is running on.

Now run
```bash
    $ ionic emulate ios
```
This command compiles and packages the app and deploys it to the iOS simulator which is started
if necessary.

If you want to install/run the app on a real iOS device you need an Apple developer's licence.

To access the data hosted in the MusicService this service mus be running.

For more information see:

- http://cordova.apache.org/docs/en/latest/guide/platforms/ios/index.html
- http://ionicframework.com/docs/cli/run.html


05. How to run the app in an Android emulator
---------------------------------------------

To run the app in an Android emulator ...

- you must have the JDK (1.7 or higher) installed
- JAVA_HOME just be set to the loacation where your JDK is installed.
- you must have the Android SDK installed
  (You also can install Android Studio which contains the Android SDK.)
- ANDROID_HOME just be set to the loacation where your Android SDK is installed.
- you must have at least one virtual device configure in the Android Virtual Device Manager.

If these preconditions are met use
```bash
    $ ionic state reset
```
to install the configured platforms (ios and android) and cordova plugins.

Before starting the emulator find the IP address of your computer where MusicService is running.
In www/js/services.js (line 6) change the "defaultURL" accordingly. Do not use "localhost"
or "127.0.0.1", use the external IP address of the host MusicService is running on.

To prepare your device ...

- go to settings/security and activate side loading
  (= installing apps from other resources than Google Play Store)
- in settings activate developer options
- in settings/developer options activate USB debugging
- connect your Android device to the same WiFi network your PC or Mac is connected to
- connect your Android device with a USB cable to your PC or Mac

Everything prepared run the command
```bash
    $ ionic run android
```
This command compiles and packages the app and deploys it to the Android device.

Then invoke
```bash
    $ ionic resources
```
in order to generate icon and splash resources for different sizes in ios and Android.

To access the data hosted in the MusicService this service mus be running.

For more information see:

- http://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html
- http://ionicframework.com/docs/cli/run.html


06. How to run the app on a real Android device
-----------------------------------------------

To run the app on an Android device ...

- you must have the JDK (1.7 or higher) installed
- JAVA_HOME just be set to the loacation where your JDK is installed.
- you must have the Android SDK installed
  (You also can install Android Studio which contains the Android SDK.)
- ANDROID_HOME just be set to the loacation where your Android SDK is installed.
- you must have at least one virtual device configure in the Android Virtual Device Manager.

If these preconditions are met use
```bash
    $ ionic state reset
```
to install the configured platforms (ios and android) and cordova plugins.

Then invoke
```bash
    $ ionic resources
```
in order to generate icon and splash resources for different sizes in ios and Android.

Before starting the emulator find the IP address of your computer where MusicService is running.
In www/js/services.js (line 6) change the "defaultURL" accordingly. Do not use "localhost"
or "127.0.0.1", use the external IP address of the host MusicService is running on.

Now run
```bash
    $ ionic emulate android
```
This command compiles and packages the app and deploys it to the Android emulator which is started
if necessary.

You can control the app and view the apps log on the connected device from within Chrome using the URL:

- chrome://inspect

For more information see:

- http://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html
- http://ionicframework.com/docs/cli/run.html


07. How to view/edit the project source code
--------------------------------------------

Use any text editor to view or edit the code.
"Brackets" is a very good choice for editing HTML, CSS and JavaScript.

See/download:

    http://brackets.io


08. Why Ionic + Cordova?
------------------------

There are other solutions/frameworks available for hybrid mobile development.
I chose the Ionic/Cordova combination because these two are currently the
leading tandem in the market for multi-platform mobile development.




Ionic App Base
=====================

A starting project for Ionic that optionally supports using custom SCSS.

## Using this project

We recommend using the [Ionic CLI](https://github.com/driftyco/ionic-cli) to create new Ionic projects that are based on this project but use a ready-made starter template.

For example, to start a new Ionic project with the default tabs interface, make sure the `ionic` utility is installed:

```bash
$ npm install -g ionic
```

Then run:

```bash
$ ionic start myProject tabs
```

More info on this can be found on the Ionic [Getting Started](http://ionicframework.com/getting-started) page and the [Ionic CLI](https://github.com/driftyco/ionic-cli) repo.

## Issues
Issues have been disabled on this repo, if you do find an issue or have a question consider posting it on the [Ionic Forum](http://forum.ionicframework.com/).  Or else if there is truly an error, follow our guidelines for [submitting an issue](http://ionicframework.com/submit-issue/) to the main Ionic repository.
