MusicWebApp-Play-Scala-PlayWS-NoAuth
====================================


Table of Contents
-----------------

01. What is the "MusicWebApp"?
02. Prerequisites to build, test and run the project
03. How to test and run (from the command line)
04. How to import the project into IntelliJ IDEA
05. How to import the project into (Eclipse based) ScalaIDE
06. MusicWebApp Architecture
07. What the MusicWebApp implementation provides
08. Why Play Framework?
09. Why Scala?


01. What is the "MusicWebApp"?
-------------------------------

This simple App is a "Music Web Application", it is
- a web application running in the browser
- with web pages being rendered on the Play server.

The MusicWebApp manages music Performers and Recordings. It is a RESTful
web service client to the web service MusicService.
The MusicWebApp cannot do any work without MusicService being started.

With MusicWebApp the user can perform the following operations in the browser:

- add, change, delete and query Performers and Recordings
- listen to Recordings

The App is a small case study using ...

- The Play Framework as containerless Web framework
- Scala as implementation language
- The Play web service library to access the MusicService
- Bootstrap CSS to enhance the look of the web pages
- Currently no client authentication to access performers or recordings
- Currently no client roles with different access priviledges

In the current stage the App doesn't implement any security features.
It offers its interface via HTTP (no HTTPS) and does not require
the user to log in before accessing recordings on the server.

The project build is based on SBT (Scala Build Tool).


02. Prerequisites to build, test and run the project
----------------------------------------------------

- Installation of JDK 1.8

- The JAVA_HOME environment variable must be set to the installation directory.
  (On a Windows System this is typically somthing like
  C:\Program Files\Java\jdk1.8.0_66

- The bin sub-directory $JAVA_HOME/bin ( %JAVA_HOME%\bin under Windows ) must
  be included in the PATH variable of the system.

To check the correct installation of the JDK, open a new command window after
the JDK installation.

Enter the command:
```bash
    $ java -version
```
This command gives you the version number of the Java Runtime found in your PATH.
This shoud show up something like ...
```bash
    java version 1.8.0_66"
```
Enter another command:
```bash
    $ javac       # invokes the java compiler
```
This command should show up the usage of the java compiler with a list of options.
If you see this you know that the Java compiler is installed correctly and is found in the class path.


03. How to test and run (from the command line)
-----------------------------------------------

In order to run the tests or the web application MusicService must already be running.
The MusicWebApp tests expect the MusicService running on port 9000. (This is at present
not configurable.)

After having lauched MusicService go to the MusicWebApp project directory and lauch
the following commands:
```bash
[unix/osx] $  ./activator test                    # runs the tests
[windows ] >  .\activator.bat test                # runs the tests
```
```bash
[unix/osx] $  ./activator run [ -Dhttp.port=<some_port> ]         # launches the app
[windows ] >  .\activator.bat run [ -Dhttp.port=<some_port> ]     # launches the app
```
If you don't specify the http.port the App uses port 9000 as default.
After having launched the App you can access with your browser at http://localhost:9000

If you have already started MusicService on port 9000, use a different port,
e.g. 9001 as port when starting up MusicWebApp.
```bash
[unix/osx] $  ./activator run -Dhttp.port=9001         # launches the app on port 9001
[windows ] >  .\activator.bat run -Dhttp.port=9001     # launches the app on port 9001
```


04. How to import the project into IntelliJ IDEA
------------------------------------------------

[unix/osx] $  ./activator gen-idea        # generates IntelliJ project files
[windows ] >  .\activator.bat gen-idea    # generates IntelliJ project files

- Open IntelliJ (with Scala Plugin installed)
- Select "File -> Open Project"
- Select "Select the project directory"


05. How to import the project into (Eclipse based) ScalaIDE
-----------------------------------------------------------

[unix/osx] $  ./activator eclipse        # generates Eclipse project files
[windows ] >  .\activator.bat eclipse    # generates Eclipse project files

- Open ScalaIDE
- Select "File -> Import..."
- Select "General -> Existing Projects into Workspace"
- In "Select as root directory" enter the path to the project directory
- Click "Finish"


06. MusicWebApp Architecture
-----------------------------

The project uses the MVC (Model View Controller) pattern. The roles of this pattern
are implemented in the respective packages: musicsvc.models, musicsvc.views and
musicsvc.controllers.

Incoming HTTP requests are routed to the controllers. Routing is configured in
conf/routes. This file specifies (in three columns)
- the request method of a HTTP request (GET, POST, PUT, DELETE)
- the URL path of a supported HTTP request
- the controller method (and parameters) where the request is routed to

There is just one controllers:
- musicWebApp.controllers.WebApplication provides the web application
  to be run in the browser.

The controllers access the web service layer. Class
musicWebApp.ws.WsApi provides the interface for web service access.

All application classes are located in the "app" directory.
All test classes are located in the "test" directory.


07. What the MusicWebApp implementation provides
-------------------------------------------------

- A data model with 2 entities: Recording and Performer.
  (implemented in musicWebApp.models.Models.scala)

- The web service layer in class musicWebApp.ws.WsApi provides RESTful access
  to MusicService.

- The web service layer is intensively tested in musicWebApp.test.WsApiSpec

- Class musicWebApp.controllers.WebApplication implements the web application and
  accesses the web service layer to store and load Recordings in the MusicService.
  Together with the views in package musicWebApp.views it provides the user interface
  according to the MVC pattern.

- Class musicWebApp.test.WebApplicationSpec contains some basic test which have to be
  extended.


08. Why Play Framework?
-----------------------

Why did I choose the Play Framework for the implementation?

    1. Play is containerless.
    2. Play is reactive.

Containerless: Traditional web containers or application servers use the
"one thread per request" model. If a request blocks during I/O (typically while
accessing the database, a file or another web service) the thread is blocked.
If there is heavy load on the service (thousands of requests per second)
traditional web containers run out of resources. Play doesn't.

Reactive: Play is light weight and reactive by design, traditional web containers
are not. The framework never blocks during a request. Thus it is ideal for concurrent
and non-blocking processing. Every request is handled asynchronously using Scala Futures.
The developer must fulfill this non-blocking promise as well. In the implementation of
request handlers (Actions in the controller classes) he/she must never block
(e.g. never wait for a database request to return a result).

Play itself is written in Scala, but it has a Scala and a Java API. Thus you can
write your Play web app in Scala or in Java.


09. Why Scala?
--------------

Functional Programming (FP) and Object-Oriented Programming (OOP) are no contradiction.
These paradigms complement each other. Functional Programming can make programs clearer
and more concise.

Java has obtained some functional features in version 8 (about 20 years after it's birth).
Scala is designed as a functional language from the beginning. Thus it supports FP much better.
Functional features are not an add-on (as in Java) but the foundation of Scala.

Scala is compatible with Java. It runs on the JVM. Every existing Java library can be used
in Scala as well. Scala can be used in mixed projects partly written in Java and partly in Java.

If you know Scala ...

- Scala code is shorter and more concise.
- Scala code is clearer, i.e. easier to understand.
- Scala code is easier to maintain and less error prone.
- The Scala compiler supports developers by being much pickier than the Java compiler.
  Type safety is a great plus in large projects. Thus many errors can be detected at compile
  time. This avoids many debug sessions.
- Using case classes instead of value objects (Java Beans) you must no longer define
  getters and setters, hashcode() and equals().
- Pattern matching avoids downcasting objects in many situations.
- Scala is designed to avoid shared mutable state which very often is the basic problem
  of traditional concurrent Java programs. Furthermore, if you use Scala collections
  you use immutable collections by default.
- Scala is much better prepared for asynchronous and concurrent programming than Java is.

Scala learned from the deficiencies of Java, e.g.
- No checked Exceptions force you to handle or to propagate them thus cluttering your
  method signatures.
- You (almost) never deal with NPEs if you consequently use the Option type. (Java 8 also
  got a new type "Optional" persueing the same purpose.)
- Exception handling is much more elegant with Try objects.
- and much more ...

Java is fun.
Scala is more fun ... and more productive.

