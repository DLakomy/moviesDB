# Movies DB

The point of the exercise is to write an application with a REST-like API,
OpenAPI documentation, JSON encoding, a database. I've decided to use these libraries:
- Tapir (to easily generate OpenAPI)
- Doobie (seems to be popular and is close to plain SQL, which is my preference)
- Circe (seems to be popular, I thought it's worth to play with it)
- SQLite (I wanted something very simple) 
- Flyway (I don't know any good alternative)

I'm focusing on writing an application with a good overall structure
and getting to know the libraries I've chosen.
Writing a useful app is not my objective this time, so I made some simplifications,
like using SQLite or simplifying the data model.

## How to build?

### Fatjar
TBD

### Native image
TBD

### Docker
TBD

## TODO
- [x] Endpoints
- [X] Move model to a dedicated file/package
- [X] Service + Repository algebras
- [X] Service impl.
- [X] Schema (Flyway)
- [X] Users repo (SQLite)
- [ ] Movies repo (SQLite)
- [ ] Endpoints tests
- [X] Authentication (at this moment the movies should belong to a user)
- [ ] parametrisation of db location, host and server
- [ ] Fatjar
- [ ] Docker (db on a volume!)

## Worth doing but I don't plan to

- E2E tests
- better data model (especially Episodes and its lack of id...)
- some validations
- use H2 instead of SQLite (I think it would be more convenient)
- proper logging
- separate DTO and domain (not necessary in such a simple app)

Well, I need to take a break from coding, sometimes :D
I would pursue these points if it were a proper app (not just an exercise)
or something I have no idea how to implement.
