package moviesdb.testUtils

import moviesdb.domain.*
import moviesdb.domain.Movies.*

// no need to test series at the moment, the logic is the same
val standaloneTemplate: NewStandalone = NewStandalone("Whatever", ProductionYear(2023))

private val uid1 = UserId(1)
private val uid2 = UserId(2)

val user1movies: List[Movie] = List(
  standaloneTemplate.withId(MovieId(1)),
  standaloneTemplate.withId(MovieId(2))
)

val user2movies: List[Movie] = List(
  standaloneTemplate.withId(MovieId(3)),
  standaloneTemplate.withId(MovieId(4))
)

private val user1rows: Vector[DbRow] = user1movies.toVector.map(mov => DbRow(uid1, mov))

private val user2rows: Vector[DbRow] = user2movies.toVector.map(mov => DbRow(uid2, mov))

val exampleMovies: Vector[DbRow] = user1rows ++ user2rows
