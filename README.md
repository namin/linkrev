linkrev
=======

### Local Deployment ###

```
play console
  new play.core.StaticApplication(new java.io.File("."))
  controllers.Application.create_graph()
play
  run
```

### Heroku Deployment ###

```
heroku run sbt play console
  new play.core.StaticApplication(new java.io.File("."))
  controllers.Application.create_graph()
```
