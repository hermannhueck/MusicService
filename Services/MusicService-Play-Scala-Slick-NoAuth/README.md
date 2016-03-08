MusicService-Play-Scala-Slick-NoAuth
====================================


Table of Contents
-----------------

01. What is the "MusicService"?
02. Prerequisites to build, test and run the project
03. How to test and run (from the command line)
04. How to import the project into IntelliJ IDEA
05. How to import the project into (Eclipse based) ScalaIDE
06. MusicService Architecture
07. What the MusicService implementation provides
08. What the implementation does not yet provide - Intended next implementation steps
09. Why Slick?
10. Why Play Framework?
11. Why Scala?


01. What is the "MusicService"?
-------------------------------

This App is a "Music Service", it is
- a web service application providing a data interface with a restful JSON API
- a web application running in the browser

The Music Service manages Performers and Recordings. Management implies
CRUD operations on Performers and Recordings, up- and download of music data
(mp3 files) as well as assigning Performers to Recordings or Recordings to
Performers and the removal of these assignments.

One entity is a __Performer__ with the following attributes:
- ___name___:             the Performer's name
- _performerType_:    one of "Soloist", "Ensemble", "Conductor"
- _recordings_:       the Recordings this Performer is performing in

The other entity is a "Recording" with these attributes:
- _title_:            the Recording's title
- _composer_:         the composer of the music contained in the recording
- _year_:             the year in which the music has bin recorded
- _performers_:       the Performers by whom the Recording was performed

The Music Service allows a client (web service client or web client) to perform
the following operations:

- to add, change, delete and query Performers and Recordings
- download or stream music data to the client

The App is a small case study using ...

- the Play Framework as containerless Web framework
- Scala as implementation language
- Slick as persistence layer
- an H2 database to persist the entities
- Currently no client authentication to access Performers and Recordings
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

Enter the command:       java -version

This command gives you the version number of the Java Runtime found in your PATH.
This shoud give you something like "java version 1.8.0_66"

Enter another command:      javac       # invokes the java compiler

This command should show up the usage of the java compiler with a list of options.


03. How to test and run (from the command line)
-----------------------------------------------

Go to the project directory and lauch the following commands:

[unix/osx] $  ./activator test                    # runs the tests
[windows ] >  .\activator.bat test                # runs the tests

[unix/osx] $  ./activator run [ -Dhttp.port=<some_port> ]         # launches the app
[windows ] >  .\activator.bat run [ -Dhttp.port=<some_port> ]     # launches the app

If you don't specify the http.port the App uses port 9000 as default.
After having launched the App you can access with your browser at http://localhost:9000


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


06. MusicService Architecture
-----------------------------

The project uses the MVC (Model View Controller) pattern. The roles of this pattern
are implemented in the respective packages: musicsvc.models, musicsvc.views and
musicsvc.controllers.

Incoming HTTP requests are routed to the controllers. Routing is configured in
conf/routes. This file specifies (in three columns)
- the request method of a HTTP request (GET, POST, PUT, DELETE)
- the URL path of a supported HTTP request
- the controller method (and parameters) where the request is routed to

There are currently two controllers:
- musicsvc.controllers.WebService provides the web service interface.
- musicsvc.controllers.WebApplication provides the web application
  to be run in the browser.

The controllers access the persistence layer. Class
musicsvc.persistence.Repository provides the interface for repository access
and delegates to musicsvc.persistence.RepositoryPerformers and
musicsvc.persistence.RepositoryRecordings. RepositoryPerformers implements
access to persistent Performer entities. RepositoryRecordings impements access
to persistent Recording entities.

Data entities are stored in an H2 in-memory SQL database with 3 tables:
- PERFORMERS: this table stores entities of type Performer
  (see class musicsvc.models.Performers)
- RECORDINGS: this table stores entities of type Recording
  (see class musicsvc.models.Recordings)
- RECORDINGS_PERFORMERS: this table stores the many-to-many relationship
  between Recordings and Performers
  (see class musicsvc.models.RecordingsPerformers)

All application classes are located in the "app" directory.
All test classes are located in the "test" directory.


07. What the MusicService implementation provides
-------------------------------------------------

- A data model with 2 entities: Recording and Performer. Recordings and Performer
  are in a many-to-many relationship: One Recording may have many Performers and
  one Performer may perform in many Recordings.
  (implemented in musicsvc.models.Models.scala)

- The persistence layer in class musicsvc.persistence.Repository provides access
  to entities of type Recording which are stored in the table "RECORDINGS" and to
  entities of type Performer which are stored in table "PERFORMERS". Table
  "RECORDINGS_PERFORMERS" is a join table which models the many-to-many
  relationship between Performer entities and Recording entities.

- The persistence layer is intensively tested in musicsvc.test.RepositoryPerformersSpec
  and musicsvc.test.RepositoryRecordingsSpec

- Class musicsvc.controllers.WebService implements the RESTful web service interface and
  accesses the persistence layer to store and load Recordings and Performers.

- Extensive tests are available for the RESTful web service API. These tests
  are implemented in musicsvc.test.WebServiceSpec.scala in the test directory.

- Class musicsvc.controllers.WebApplication implements the web application and
  accesses the persistence layer to store and load Recordings. Together with
  the views in package musicsvc.views it provides the user interface according
  to the MVC pattern.

- Class musicsvc.test.WebApplicationSpec contains some basic test which have to be
  extended.


08. What the implementation does not yet provide - Intended next implementation steps
-------------------------------------------------------------------------------------

- Add more tests for the web application in musicsvc.test.WebApplicationSpec
  (using Selenium Tests)

- The web application currently uses the Repository interface to access Performer
  and Recording entities. This should be refactored to use the RESTful web service
  interface to access these entities.

- Reimplement the web application and make it a "single page web app" which uses
  the RESTful web service API with JavaScript (and/or CoffeeScript, TypeScript,
  jQuery, AngularJS, bootstrap) to access Recordings and Performers


09. Why Slick?
--------------

Why did I choose the Slick for data access?

    1. Slick takes an FRM aproach to access the data model
    2. Slick is reactive.

FRM (Functional Relational Mapping): With FRM you can treat data sets almost in
the same way as Scala collections. Thus data access is integrated into Scala
without frictions.

Reactive: Database access is asynchronous and non-blocking from the bottom.
When you run a database operation (query, insert update or delete) Slick never
returns a result. Instead it returns a Future of a result. Thus Slick is a perfect
fit for Scala reactive applications written with Akka or Play.


10. Why Play Framework?
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


11. Why Scala?
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
