Deployment Notes
================

To generate the neo4j graph database, run the following in a console:
```
new play.core.StaticApplication(new java.io.File("."))
controllers.Application.create_graph()
```

Here is how to bring up the play console in Heroku:
```
heroku run sbt play console
```
