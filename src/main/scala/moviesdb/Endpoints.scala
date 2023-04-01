package moviesdb

import sttp.tapir.*
import Movies.*
import cats.effect.IO
import io.circe.{Decoder as CDecoder, Encoder as CEncoder}
import io.circe.derivation.Configuration as CirceConfiguration
import sttp.model.StatusCode
import sttp.tapir.generic.{Configuration, Derived}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.Schema.annotations.*
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

object Movies:
  trait Identifiable:
    def id: Int

  case class ProductionYear(year: Int) extends AnyVal

  // of course it should have a variant with ID, but I want to simplify the exercise
  case class Episode(title: String, year: ProductionYear, number: Int)

  sealed trait NewMovie
  case class NewStandalone(title: String, year: ProductionYear) extends NewMovie
  case class NewSeries(title: String, episodes: List[Episode]) extends NewMovie

  sealed trait Movie extends Identifiable
  case class Standalone(id: Int, title: String, year: ProductionYear) extends Movie
  case class Series(id: Int, title: String, episodes: List[Episode]) extends Movie

  private val discriminatorFieldName = "objectType"
  // circe start
  given CirceConfiguration = CirceConfiguration.default.withDiscriminator(discriminatorFieldName)
  given CEncoder[ProductionYear] = CEncoder.encodeInt.contramap(_.year)
  given CDecoder[ProductionYear] = CDecoder.decodeInt.map(ProductionYear.apply)
  given CDecoder[Episode] = CDecoder.derivedConfigured
  given CEncoder[Episode] = CEncoder.AsObject.derivedConfigured
  given CEncoder[NewMovie] = CEncoder.AsObject.derivedConfigured
  given CDecoder[NewMovie] = CDecoder.derivedConfigured
  given CEncoder[Movie] = CEncoder.AsObject.derivedConfigured
  given CDecoder[Movie] = CDecoder.derivedConfigured
  // circe stop

  // tapir start
  given Configuration =
    Configuration.default.withDiscriminator(discriminatorFieldName)
  given Schema[ProductionYear] =
    Schema(SchemaType.SInteger()).validate(Validator.min(1800).contramap(_.year))
  given Schema[Episode] =
    summon[Derived[Schema[Episode]]].value.modify(_.number)(_.validate(Validator.min(1)))
  given Schema[NewMovie] =
    summon[Derived[Schema[NewMovie]]]
      .value.modifyUnsafe(discriminatorFieldName)(_.copy(description = Some("discriminator")))
  given Schema[Movie] =
    summon[Derived[Schema[Movie]]]
      .value.modifyUnsafe(discriminatorFieldName)(_.copy(description = Some("discriminator")))
  // tapir stop
