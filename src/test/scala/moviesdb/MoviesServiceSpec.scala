package moviesdb

import cats.Id
import moviesdb.domain.UserId
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
  var service: MoviesServiceAlgebra[Id] = null

  override def beforeEach(context: BeforeEach): Unit =
    val repo = MoviesRepoMockup[Id](exampleMovies)
    service = MoviesService(repo)

  test("Should list only movies owned by this user") {
    val obtained1 = service.getMoviesForUser(UserId(1))
    assert(obtained1 == user1movies)

    val obtained2 = service.getMoviesForUser(UserId(2))
    assert(obtained2 == user2movies)
  }

  test("Should return a movie by id only if the user owns it") {
    // TODO - nonexisting
    // TODO - existing + wrong user
    // TODO - existing
    ???
  }

  test("Should correctly create a movie") {
    ???
  }

  test("Should delete a movie and indicate if it existed before the deletion") {
    ???
  }

  test("Should update a movie if the user owns it, NotFound otherwise") {
    ???
  }

  test("Should reject update on id mismatch") {
    ???
  }
