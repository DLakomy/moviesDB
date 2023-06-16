# Movies DB
[![Scala CI](https://github.com/DLakomy/moviesDB/actions/workflows/scala.yml/badge.svg)](https://github.com/DLakomy/moviesDB/actions/workflows/scala.yml)

The point of the exercise was to write an application with a REST-like API,
OpenAPI documentation, JSON encoding and a database. I've decided to use these libraries:
- Tapir (to easily generate OpenAPI docs)
- Doobie (seems to be popular and is close to plain SQL, which is my preference)
- Circe (seems to be popular, I thought it's worth to play with it)
- SQLite (I wanted something very simple, H2 could be a good candidate as well) 
- Flyway (I don't know any good alternative, I haven't worked with Liquibase)

I've focused on writing an application with a good overall structure
and getting to know the libraries I've chosen.
Writing a useful app hasn't been my objective this time, so I've made some simplifications,
like using SQLite or creating a simple, suboptimal the data model. The one I've created
allowed me to create some interesing queries (actually quite common, but rather new to me in
case of Doobie), like fetching master+detail and fetching data from different tables in one
transaction.

## How to build?

You can use `sbt compile`, but it's usually easier to work with a Fatjar or Docker.

### Fatjar
`sbt assembly`
Read the output carefully, it will show you where is the Fatjar (ie. the jar archive without external dependencies).

### Docker
TBD

## How to run?
Provide these env variables:
- `MOVIESDB_HOST` - by default `localhost`
- `MOVIESDB_PORT` - by default `8080`
- `MOVIESDB_DBPATH` - by default `movies.db` (pun intended, see the project name)
- `MOVIESDB_DBPOOLSIZE` - by default `32` (I'm curious if SQLite can handle this gracefully...)

Invoke `sbt run` and go to http://localhost:8080/docs (or wherever it is in case you're using a nondefault configuration).
If you've the fatjar, use `java -jar /path/to/this/jar/archive.jar`.

## Lessons learned
- how to create endpoints described with Tapir, involving autogenerated OpenAPI schemas
- how to use Doobie in an application and its tests
- how to write unit tests with in memory implementations, in an OOP/FP lang (I usually write in procedural langs, it's a different kettle of fish)
- how to use Flyway in a Scala app (before I've used it only via Maven or CLI)
- how to use Circe (not much to learn here, it just works when configured correctly)

## Worth doing but it's out of scope

- endpoints tests (Tapir provides some useful functionalities for these)
- E2E tests (and maybe test service layer and repos together in UTs,
they are mean't to be used together anyway; it would be less code to maintain),
- load tests (to be sure that SQLite can handle that..., anyway it wouldn't by my choice for a proper app, ofc.)
- better data model (especially Episodes and its lack of id...)
- some validations
- proper logging
- proper configuration parser
- separate DTO and domain (not necessary in such a simple app)
- scalafmt (I'll learn to use one day)

Why out of scope? Well, I need to take a break from coding, sometimes :D
I would pursue these points if it were a proper app (not just an exercise)
or something I have no idea how to implement.

## PS How to add a user?
Well... You can use the ones from `V1.1__example_users.sql` or insert another one somehow. That's it for now ¯\_(ツ)_/¯
