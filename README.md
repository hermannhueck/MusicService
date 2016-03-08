MusicService + Clients
======================

A case study for a service implemented with Scala, Akka (not yet) and Play
using clients implemented with different technologies

The MusicService is designed to manage two kinds of entities:

- __Recording__ entities are music recordings
- __Performer__ entities are performers who perform in the recordings. A Performer can be either a "Soloist" or an "Ensemble" or a "Conductor.
- __Many-to-Many__ describes the type of relationship between these two entities. (A performer can perform in many recordings. A recording can have many performers.)

The "Services" subdirectory contains different implementations of the MusicService web service.
Currently there is one implementation written with Play in Scala.

- RESTful Web Service implementation with Play in Scala persisting data in an SQL database with Slick (implemented)

But I've some ideas for future implementations.

- RESTful Web Service implementation with Play in Scala persisting data in the MongoDB NoSQL database (not implemented)
- RESTful Web service implementation with Akka-HTTP in Scala persisting data in the Cassandra NoSQL database (not implemented)
- RESTful Web service implementation with NodeJS persisting data in the MongoDB NoSQL database (not implemented)

The "Clients" subdirectory contains different implementions of the MusicService REST clients.
Currently there are three implementations.

- Web App in Play using the web service client library of Play (implemented)
- Single Page Web App in HTML5, CSS and JavaScript using AngularJS and Bootstrap accessing the web service in JavaScript (implemented)
- Hybrid Mobile App in HTML5, CSS and JavaScript using Ionic, AngularJS and Cordova accessing the web service in JavaScript (implemented)

But I've ideas for some more ...

- Web App in Play using the web service client library of Akka-HTTP (not implemented)
- Web App in Play using Retrofit (with RxScala) as web service client library (not implemented)
- Android App using Retrofit and RxJava as web service client library (not implemented)

Work is in progress. I'll add bug fixes, improvements, refactorings and more projects
depending on the time I can spare to implement them.
No promises. No time lines. This is just a fun job.

Note: There is a README in each project directory containing some brief informations,
which help you to unterstand the fundamentals of the architecture as well as how to compile,
test and run the respective service or application. These README do not contain an in-depth discussion

Have fun.

Hermann
