package moviesdb.endpoints

import cats.Id
import moviesdb.domain.{Movies, User, UserId, UserName}
import moviesdb.movies.{ApiErrorOr, MoviesServiceAlgebra}
import moviesdb.users.UsersServiceAlgebra
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import java.util.UUID

// not a full suite, just to reproduce an error I've encountered
class MovieEndpointsSpec extends munit.FunSuite:

  // there was HTTP 500, mishandler error mapping in the delete endpoint
  test("Delete on a nonexistent movie should return HTTP 404") {

    val serviceMockup = new MoviesServiceAlgebra[Id]:
      override def deleteMovie(movieId: Movies.MovieId, userId: UserId): Id[Option[Unit]] = None
      // needed only for this thing to compile
      override def getMoviesForUser(id: UserId): Id[List[Movies.Movie]] = ???
      override def getMovie(movieId: Movies.MovieId, userId: UserId): Id[Option[Movies.Movie]] = ???
      override def createMovie(movie: Movies.NewMovie, userId: UserId): Id[ApiErrorOr[Movies.Movie]] = ???
      override def updateMovie(movieId: Movies.MovieId, updatedMovie: Movies.Movie, userId: UserId): Id[ApiErrorOr[Unit]] = ???

    val authMockup = new UsersServiceAlgebra[Id]:
      override def getUser(userName: UserName, password: String): Id[Option[User]] =
        Some(User(UserId(UUID.randomUUID()), UserName("Whoever")))

    val endpoints = MovieEndpoints[Id](serviceMockup, authMockup)

    val backendStub = TapirStubInterpreter(SttpBackendStub.synchronous)
      .whenServerEndpointsRunLogic(endpoints.apiEndpoints)
      .backend()

    val response = basicRequest
      .auth.basic("Whoever", "whatever")
      // deleting a nonexistent movie
      .delete(uri"http://localhost:8080/movies/6b907b32-2e08-4f28-b904-4a580da5b632")
      .send(backendStub)

    assertEquals(response.code, StatusCode.NotFound)
  }
