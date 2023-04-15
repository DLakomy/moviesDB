package moviesdb

import cats.Monad
import cats.syntax.functor.*
import io.circe.derivation.Configuration as CirceConfiguration
import io.circe.{Decoder as CDecoder, Encoder as CEncoder}
import moviesdb.domain.*
import moviesdb.domain.Movies.*
import moviesdb.movies.{MoviesServiceAlgebra, UpdateError}
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.{Configuration, Derived}
import sttp.tapir.json.circe.*
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class Endpoints[F[_]](service: MoviesServiceAlgebra[F])(using F: Monad[F]):

  // example
  val exampleMovies: List[Movie] = List(
    Standalone(MovieId(123), "Movie1", ProductionYear(2007)),
    Series(MovieId(456), "Series3", List(Episode("EpI", ProductionYear(2008), 1), Episode("EpII", ProductionYear(2005), 2)))
  )
  private val exampleMovie: Movie = exampleMovies.head
  private val newExampleMovie: NewMovie = NewStandalone("Movie1", ProductionYear(2007))

  private val moviesEndpoint: PartialServerEndpoint[UsernamePassword, User, Unit, String, Unit, Any, F] = endpoint
    .tag("Movie actions")
    .in("movies")
    .securityIn(auth.basic[UsernamePassword]())
    .errorOut(statusCode(StatusCode.Forbidden).description("Unauthorized").and(stringBody))
    .serverSecurityLogic(credentials => F.pure(Either.cond(credentials.password.getOrElse("")=="AQQ", User(UserId(66), "testUser"), "Unauthorized")))

  private val moviesListing = moviesEndpoint
    .get
    .out(jsonBody[List[Movie]].example(exampleMovies))
    .serverLogicSuccess(user => _ => service.getMoviesForUser(user.id))

  private val getMovieById = moviesEndpoint
    .get
    .in(path[Int].name("id"))
    .out(jsonBody[Movie].example(exampleMovie))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Movie not found").and(stringBody)))
    .serverLogic(user => id => service.getMovie(MovieId(id), user.id).map(_.toRight("Not found")))

  private val createMovie = moviesEndpoint
    .post
    .in("create")
    .in(jsonBody[NewMovie].description("A movie to add").example(newExampleMovie))
    .out(statusCode(StatusCode.Created).description("Successfully created").and(jsonBody[Movie].example(exampleMovie)))
    .serverLogic(user => newMovie => service.createMovie(newMovie, user.id))

  private val deleteMovieById = moviesEndpoint
    .delete
    .in(path[Int].name("id"))
    .out(statusCode(StatusCode.NoContent).description("Successfully deleted"))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Not found")))
    .serverLogic(user => id => service.deleteMovie(MovieId(id), user.id).map(_.toRight("Not found")))

  private val updateMovieById = moviesEndpoint
    .put
    .in(path[Int].name("id"))
    .in(jsonBody[Movie].example(exampleMovie))
    .out(statusCode(StatusCode.NoContent).description("Successfully updated as instructed (no need to fetch)"))
    .errorOutVariant(oneOfVariant(statusCode.description(StatusCode.NotFound, "Not found").description(StatusCode.BadRequest, "Malformed message or id mismatch")))
// TODO fix this and somehow change status code, depending on an error type
//    .serverLogic(user => (id, updatedMovie) => service.updateMovie(MovieId(id), updatedMovie, user.id))
    .serverLogic(user => (id, updatedMovie) => F.pure(Either.cond(id < 0, (), (UpdateError.IdMismatch.toString))))

  val apiEndpoints: List[ServerEndpoint[Any, F]] = List(
    moviesListing,
    getMovieById,
    createMovie,
    updateMovieById,
    deleteMovieById)

  val docEndpoints: List[ServerEndpoint[Any, F]] = SwaggerInterpreter()
    .fromServerEndpoints[F](apiEndpoints, "Movies DB", "1.0.0")

  val all: List[ServerEndpoint[Any, F]] = apiEndpoints ++ docEndpoints
