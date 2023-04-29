package moviesdb

import cats.Id
import moviesdb.movies.MoviesRepoAlgebra
import moviesdb.testUtils.*

/*
trait MoviesServiceAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  def createMovie(movie: NewMovie, userId: UserId): F[Either[ErrorInfo, Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[ErrorInfo, Unit]]

*/

// TODO https://scalameta.org/munit/docs/fixtures.html
class MoviesServiceSpec extends munit.FunSuite:

  var repo: MoviesRepoAlgebra[Id] = null

  override def beforeEach(context: BeforeEach): Unit =
    repo = MoviesRepoMockup(Vector.empty)

  test("Should list only movies owned by this user") {
    ???
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
