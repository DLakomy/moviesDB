package moviesdb

import cats.effect.IO
import io.circe.derivation.Configuration as CirceConfiguration
import io.circe.{Decoder as CDecoder, Encoder as CEncoder}
import moviesdb.domain.*
import moviesdb.domain.Movies.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.{Configuration, Derived}
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Endpoints:

  // example
  val exampleMovies: List[Movie] = List(
    Standalone(123, "Movie1", ProductionYear(2007)),
    Series(456, "Series3", List(Episode("EpI", ProductionYear(2008), 1), Episode("EpII", ProductionYear(2005), 2)))
  )
  private val exampleMovie: Movie = exampleMovies.head
  private val newExampleMovie: NewMovie = NewStandalone("Movie1", ProductionYear(2007))

  private val moviesEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint.tag("Movie actions").in("movies")

  private val moviesListing: PublicEndpoint[Unit, Unit, List[Movie], Any] = moviesEndpoint
    .get
    .out(jsonBody[List[Movie]].example(exampleMovies))

  private val getMovieById: PublicEndpoint[Int, Unit, Movie, Any] = moviesEndpoint
    .get
    .in(path[Int].name("id"))
    .out(jsonBody[Movie].example(exampleMovie))
    .errorOut(statusCode(StatusCode.NotFound))

  private val createMovie: PublicEndpoint[NewMovie, Unit, Movie, Any] = moviesEndpoint
    .post
    .in("create")
    .in(jsonBody[NewMovie].description("A movie to add").example(newExampleMovie))
    .out(statusCode(StatusCode.Created).and(jsonBody[Movie].example(exampleMovie)))

  private val deleteMovieById: PublicEndpoint[Int, Unit, Unit, Any] = moviesEndpoint
    .delete
    .in(path[Int].name("id"))
    .out(statusCode(StatusCode.Gone))
    .errorOut(statusCode(StatusCode.NotFound))

  private val updateMovieById: PublicEndpoint[(Int, Movie), Unit, Unit, Any] = moviesEndpoint
    .put
    .in(path[Int].name("id"))
    .in(jsonBody[Movie].example(exampleMovie))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(statusCode(StatusCode.NotFound))

  private val moviesListingServerEndpoint: ServerEndpoint[Any, IO] = moviesListing.serverLogicSuccess(_ => IO.pure(exampleMovies))
  private val getMovieServerEndpoint: ServerEndpoint[Any, IO] = getMovieById.serverLogicSuccess(id => IO.pure(exampleMovie))
  private val createMovieServerEndpoint: ServerEndpoint[Any, IO] = createMovie.serverLogicSuccess(newMovie => IO.pure(exampleMovie))
  private val updateMovieByIdServerEndpoint: ServerEndpoint[Any, IO] = updateMovieById.serverLogicSuccess((id, updatedMovie) => IO.unit)
  private val deleteMovieByIdServerEndpoint: ServerEndpoint[Any, IO] = deleteMovieById.serverLogicSuccess(id => IO.unit)

  val apiEndpoints: List[ServerEndpoint[Any, IO]] = List(
    moviesListingServerEndpoint,
    getMovieServerEndpoint,
    createMovieServerEndpoint,
    updateMovieByIdServerEndpoint,
    deleteMovieByIdServerEndpoint)

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "Movies DB", "1.0.0")

  val all: List[ServerEndpoint[Any, IO]] = apiEndpoints ++ docEndpoints
