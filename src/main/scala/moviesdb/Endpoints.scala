package moviesdb

import cats.effect.IO
import io.circe.derivation.Configuration as CirceConfiguration
import io.circe.{Decoder as CDecoder, Encoder as CEncoder}
import moviesdb.domain.*
import moviesdb.domain.Movies.*
import moviesdb.movies.MoviesServiceAlgebra
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.{Configuration, Derived}
import sttp.tapir.json.circe.*
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}
import sttp.tapir.swagger.bundle.SwaggerInterpreter

// TODO convert to a class receiving service algebra
class Endpoints/*(service: MoviesServiceAlgebra)*/:

  // example
  val exampleMovies: List[Movie] = List(
    Standalone(MovieId(123), "Movie1", ProductionYear(2007)),
    Series(MovieId(456), "Series3", List(Episode("EpI", ProductionYear(2008), 1), Episode("EpII", ProductionYear(2005), 2)))
  )
  private val exampleMovie: Movie = exampleMovies.head
  private val newExampleMovie: NewMovie = NewStandalone("Movie1", ProductionYear(2007))

  private val moviesEndpoint: PartialServerEndpoint[UsernamePassword, User, Unit, Unit, Unit, Any, IO] = endpoint
    .tag("Movie actions")
    .in("movies")
    .securityIn(auth.basic[UsernamePassword]())
    .errorOut(statusCode(StatusCode.Forbidden).description("Unauthorized"))
    .serverSecurityLogic(credentials => IO.pure(if (credentials.password.getOrElse("")=="AQQ") Right(User(UserId(66), "testUser")) else Left(())))

  private val moviesListing = moviesEndpoint
    .get
    .out(jsonBody[List[Movie]].example(exampleMovies))
    .serverLogicSuccess(_ => _ => IO.pure(exampleMovies))

  private val getMovieById = moviesEndpoint
    .get
    .in(path[Int].name("id"))
    .out(jsonBody[Movie].example(exampleMovie))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Movie not found")))
    .serverLogicSuccess(user => id => IO.pure(exampleMovie))

  private val createMovie = moviesEndpoint
    .post
    .in("create")
    .in(jsonBody[NewMovie].description("A movie to add").example(newExampleMovie))
    .out(statusCode(StatusCode.Created).description("Successfully created").and(jsonBody[Movie].example(exampleMovie)))
    .serverLogicSuccess(user => newMovie => IO.pure(exampleMovie))

  private val deleteMovieById = moviesEndpoint
    .delete
    .in(path[Int].name("id"))
    .out(statusCode(StatusCode.NoContent).description("Successfully deleted"))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Not found")))
    .serverLogicSuccess(user => id => IO.unit)

  private val updateMovieById = moviesEndpoint
    .put
    .in(path[Int].name("id"))
    .in(jsonBody[Movie].example(exampleMovie))
    .out(statusCode(StatusCode.NoContent).description("Successfully updated as instructed (no need to fetch)"))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Not found")))
    .serverLogicSuccess(user => (id, updatedMovie) => IO.unit)

  val apiEndpoints: List[ServerEndpoint[Any, IO]] = List(
    moviesListing,
    getMovieById,
    createMovie,
    updateMovieById,
    deleteMovieById)

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "Movies DB", "1.0.0")

  val all: List[ServerEndpoint[Any, IO]] = apiEndpoints ++ docEndpoints
