package moviesdb

import cats.Id
import moviesdb.domain.Movies.MovieId
import moviesdb.domain.{ApiError, UserId}
import moviesdb.movies.{MoviesRepoAlgebra, MoviesService, MoviesServiceAlgebra}
import moviesdb.testUtils.*

/*
trait MoviesServiceAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  def createMovie(movie: NewMovie, userId: UserId): F[Either[ErrorInfo, Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[ErrorInfo, Unit]]

*/

class MoviesServiceSpec extends munit.FunSuite:

  // the tested service
  // recreated each time with a fresh repo
  var repo: MoviesRepoMockup[Id] = null
  var service: MoviesServiceAlgebra[Id] = null

  override def beforeEach(context: BeforeEach): Unit =
    repo = MoviesRepoMockup(exampleMovies)
    service = MoviesService(repo)

  test("Should list only movies owned by this user") {
    val obtained1 = service.getMoviesForUser(UserId(1))
    assertEquals(obtained1, user1movies)

    val obtained2 = service.getMoviesForUser(UserId(2))
    assertEquals(obtained2, user2movies)
  }

  test("Should return a movie by id only if the user owns it") {
    val nonExistent = service.getMovie(MovieId(66), UserId(1))
    assertEquals(nonExistent, None)

    // existent + wrong user
    val existentWrongUser = service.getMovie(MovieId(1), UserId(2))
    assertEquals(existentWrongUser, None)

    // existent
    val existent = service.getMovie(MovieId(1), UserId(1))
    assertEquals(existent, Some(user1movies(0)))
  }

  test("Should correctly create a movie") {
    val userId = UserId(3)

    val result = service.createMovie(standaloneTemplate, userId)
    assert(result.isRight) // only happy path atm

    val newId = result.map(_.id).getOrElse(MovieId(-1))

    // check if repo contains it
    val inRepo = repo.getMovie(newId, userId)

    assertEquals(result.toOption, inRepo)
  }

  test("Should delete a movie and indicate if it existed before the deletion") {

    assertEquals(
      service.deleteMovie(MovieId(42), UserId(1)),
      None // no such movie for this user
    )

    assertEquals(
      service.deleteMovie(MovieId(3), UserId(66)),
      None // no such user
    )

    val delMovId = MovieId(1)
    val delUsrId = UserId(1)

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

    val movId = MovieId(1)
    val modified = standaloneTemplate.copy(title="changed").withId(movId)

    val failedRes = service.updateMovie(movId, modified, UserId(2))
    assertEquals(failedRes, Left(ApiError.MovieNotFound))

    val userId = UserId(1)
    val correctRes = service.updateMovie(movId, modified, userId)
    assertEquals(correctRes, Right(()))

    val obtained = repo.getMovie(movId, userId).get
    assertEquals(obtained, modified)
  }

  test("Should reject update on id mismatch") {

    val modified = standaloneTemplate.copy(title="changed").withId(MovieId(1))
    val failedRes = service.updateMovie(MovieId(66), modified, UserId(2))

    assertEquals(failedRes, Left(ApiError.IdMismatch))
  }
