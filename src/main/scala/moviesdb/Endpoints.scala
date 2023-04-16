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

  private val moviesEndpoint: PartialServerEndpoint[UsernamePassword, User, Unit, UpdateError, Unit, Any, F] = endpoint
    .tag("Movie actions")
    .in("movies")
    .securityIn(auth.basic[UsernamePassword]())
    .errorOut(statusCode(StatusCode.Forbidden).description("Unauthorized").and(stringBody.map(_ => UpdateError.Unauthorized)(_.toString)))
    .serverSecurityLogic(credentials => F.pure(Either.cond(credentials.password.getOrElse("")=="AQQ", User(UserId(66), "testUser"), UpdateError.Unauthorized)))

  private val moviesListing = moviesEndpoint
    .get
    .out(jsonBody[List[Movie]].example(exampleMovies))
    .serverLogicSuccess(user => _ => service.getMoviesForUser(user.id))

  private val getMovieById = moviesEndpoint
    .get
    .in(path[Int].name("id"))
    .out(jsonBody[Movie].example(exampleMovie))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Movie not found").and(stringBody.map(_ => UpdateError.NotFound)(_.toString))))
    .serverLogic(user => id => service.getMovie(MovieId(id), user.id).map(_.toRight(UpdateError.NotFound)))

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

  private val updateMovieById: Full[UsernamePassword, User, (Int, Movie), UpdateError, Unit, Any, F]= moviesEndpoint
    .put
    .in(path[Int].name("id"))
    .in(jsonBody[Movie].example(exampleMovie))
    .out(statusCode(StatusCode.NoContent).description("Successfully updated as instructed (no need to fetch)"))
    // TODO wrong messages
    .errorOutVariants(
      oneOfVariantValueMatcher(StatusCode.NotFound, stringBody.description("Not found").map(_ => UpdateError.IdMismatch)(_ => "Id mismatch TODO")) { case UpdateError.IdMismatch => true },
      oneOfVariantValueMatcher(StatusCode.BadRequest, stringBody.description("Malformed message or id mismatch").map(_ => UpdateError.NotFound)(_ => "Not found TODO")) { case UpdateError.NotFound => true }
    )
    .serverLogic { user => (id, updatedMovie) =>
      service.updateMovie(MovieId(id), updatedMovie, user.id)
    }

  val apiEndpoints: List[ServerEndpoint[Any, F]] = List(
    moviesListing,
    getMovieById,
    createMovie,
    updateMovieById,
    deleteMovieById)

  val docEndpoints: List[ServerEndpoint[Any, F]] = SwaggerInterpreter()
    .fromServerEndpoints[F](apiEndpoints, "Movies DB", "1.0.0")

  val all: List[ServerEndpoint[Any, F]] = apiEndpoints ++ docEndpoints
