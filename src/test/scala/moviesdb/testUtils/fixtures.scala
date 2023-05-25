package moviesdb.testUtils

import moviesdb.domain.*
import moviesdb.domain.Movies.*

import java.util.UUID

// no need to test series at the moment, the logic is the same
val standaloneTemplate: NewStandalone = NewStandalone("Whatever", ProductionYear(2023))

val uid1 = UserId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"))
val uid2 = UserId(UUID.fromString("123e4567-e89b-42d3-a456-556642440001"))

val user1movies: List[Movie] = List(
  standaloneTemplate.withId(MovieId(1)),
  standaloneTemplate.withId(MovieId(2))
)

val user2movies: List[Movie] = List(
  standaloneTemplate.withId(MovieId(3)),
  standaloneTemplate.withId(MovieId(4))
)

private val user1rows: DbState = user1movies.toVector.map(mov => DbRow(uid1, mov))

private val user2rows: DbState = user2movies.toVector.map(mov => DbRow(uid2, mov))

val exampleMovies: DbState = user1rows ++ user2rows
