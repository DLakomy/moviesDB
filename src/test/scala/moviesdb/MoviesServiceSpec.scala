package moviesdb

import cats.Id
import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.Movies.MovieId
import moviesdb.domain.{ApiError, UserId}
import moviesdb.movies.{MoviesRepoAlgebra, MoviesService, MoviesServiceAlgebra}
import moviesdb.testUtils.*

import java.util.UUID

class MoviesServiceSpec extends munit.FunSuite:

  // the tested service
  // recreated each time with a fresh repo
  var repo: MoviesRepoMockup[Id] = null
  var service: MoviesServiceAlgebra[Id] = null

  override def beforeEach(context: BeforeEach): Unit =
    repo = MoviesRepoMockup(exampleMovies)
    service = MoviesService(repo)

  test("Should list only movies owned by this user") {
    val obtained1 = service.getMoviesForUser(uid1)
    assertEquals(obtained1, user1movies)

    val obtained2 = service.getMoviesForUser(uid2)
    assertEquals(obtained2, user2movies)
  }

  test("Should return a movie by id only if the user owns it") {
    val nonExistent = service.getMovie(mid66_nonexistent, uid1)
    assertEquals(nonExistent, None)

    // existent + wrong user
    val existentWrongUser = service.getMovie(mid1, uid2)
    assertEquals(existentWrongUser, None)

    // existent
    val existent = service.getMovie(mid1, uid1)
    assertEquals(existent, Some(user1movies(0)))
  }

  test("Should correctly create a movie") {
    // some new user id, not used in fixtures
    val userId = UserId(UUID.fromString("321e4567-e89b-42d3-a456-556642440003"))

    val result = service.createMovie(standaloneTemplate, userId)
    assert(result.isRight) // only happy path atm
    if (result.isLeft) fail("Got Left (Right expected)")

    val newId = result.map(_.id).toOption.get

    // check if repo contains it
    val inRepo = repo.getMovie(newId, userId)

    assertEquals(result.toOption, inRepo)
  }

  test("Should delete a movie and indicate if it existed before the deletion") {

    assertEquals(
      service.deleteMovie(mid66_nonexistent, uid1),
      None // no such movie for this user
    )

    assertEquals(
      service.deleteMovie(mid3, uid66_nonexistent),
      None // no such user
    )

    val delMovId = mid1
    val delUsrId = uid1

    assertEquals(
      service.deleteMovie(delMovId, delUsrId),
      Some(())
    )

    //assert final state
    val currState = repo.state
    val expectedState = exampleMovies.filterNot { row => row.movie.id == delMovId }

    assertEquals(currState, expectedState)
  }

  test("Should update a movie if it exists and the user owns it, NotFound otherwise") {

    val movId = mid1
    val modified = standaloneTemplate.copy(title="changed").withId(movId)

    val failedRes = service.updateMovie(movId, modified, uid2)
    assertEquals(failedRes, Left(ApiError.MovieNotFound))

    val userId = uid1
    val correctRes = service.updateMovie(movId, modified, userId)
    assertEquals(correctRes, Right(()))

    val obtained = repo.getMovie(movId, userId).get
    assertEquals(obtained, modified)
  }

  test("Should reject update on id mismatch") {

    val modified = standaloneTemplate.copy(title="changed").withId(mid1)
    val failedRes = service.updateMovie(mid66_nonexistent, modified, uid2)

    assertEquals(failedRes, Left(ApiError.IdMismatch))
  }
