# MusicService
A case study for Scala, Akka (not yet) and Play

The MusicService is designed to manage two kinds of entities:
- Recording entities are music recordings
- Performer entities are performers who perform in the recordings. A Performer can be either a "Soloist" or an "Ensemble" or a "Conductor.
- Many-to-Many describes the type of relationship between these two entities. (A performer can perform in many recordings. A recording can have many performers.)

The "Services" subdirectory contains different implementations of the MusicService web service.
Currently there is only one implementation written with Play in Scala. But I've some ideas
for future implementations.

- RESTful Web Service implementation with Play in Scala (implemented)
- RESTful Web service implementation with Akka-HTTP in Scala (not yet implemented)

The "Clients" subdirectory shall contain different implementions of the MusicService REST clients.
Currently there is none. But I've some ideas for future client implementations.

- Web App in Play using the web service client library of Play (not yet implemented)
- Web App in Play using the web service client library of Akka-HTTP (not yet implemented)
- Web App in Play using Retrofit (with RxScala) as web service client library (not yet implemented)
- Single Page Web App in HTML5, CSS and JavaScript accessing the web service in JavaScript (not yet implemented)
- Android App using Retrofit (with RxJava) as web service client library (not yet implemented)

Work is in progress. I'll add more projects depending on the time I can spare to implement
them. No promises. No time lines. This is just a fun job.

Note: There is a README in each project directory.

Have fun.

Hermann
