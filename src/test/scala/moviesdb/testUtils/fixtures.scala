package moviesdb.testUtils

import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.*
import moviesdb.domain.Movies.*

import java.util.UUID

// no need to test series at the moment, the logic is the same
val standaloneTemplate: NewStandalone = NewStandalone("Whatever", ProductionYear(2023))

val uid1 = UserId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"))
val uid2 = UserId(UUID.fromString("123e4567-e89b-42d3-a456-556642440001"))
val uid66_nonexistent = UserId(UUID.fromString("123e4567-e89b-42d3-a456-556642440166"))

val mid1 = MovieId(UUID.fromString("123e4567-e89b-42d3-a456-556642440011"))
val mid2 = MovieId(UUID.fromString("123e4567-e89b-42d3-a456-556642440012"))
val mid3 = MovieId(UUID.fromString("123e4567-e89b-42d3-a456-556642440013"))
val mid4 = MovieId(UUID.fromString("123e4567-e89b-42d3-a456-556642440014"))
val mid66_nonexistent = MovieId(UUID.fromString("123e4567-e89b-42d3-a456-556642440066"))

val user1movies: List[Movie] = List(
  standaloneTemplate.withId(mid1),
  standaloneTemplate.withId(mid2),
)

val user2movies: List[Movie] = List(
  standaloneTemplate.withId(mid3),
  standaloneTemplate.withId(mid4),
)

private val user1rows: DbState = user1movies.toVector.map(mov => DbRow(uid1, mov))

private val user2rows: DbState = user2movies.toVector.map(mov => DbRow(uid2, mov))

val exampleMovies: DbState = user1rows ++ user2rows
