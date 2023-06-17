package moviesdb.endpoints

import cats.Functor
import cats.syntax.functor.*
import io.circe.derivation.Configuration as CirceConfiguration
import io.circe.{Decoder as CDecoder, Encoder as CEncoder}
import moviesdb.domain.*
import moviesdb.domain.Movies.*
import moviesdb.movies.MoviesServiceAlgebra
import moviesdb.users.UsersServiceAlgebra
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

import java.util.UUID

class MovieEndpoints[F[_]: Functor](moviesService: MoviesServiceAlgebra[F], usersService: UsersServiceAlgebra[F]):

  val exampleMovies: List[Movie] = List(
    Standalone(MovieId(UUID.fromString("7f1411e0-7d13-47e6-b7f8-4a661c56dd5b")), "Movie1", ProductionYear(2007)),
    Series(MovieId(UUID.fromString("6b907b32-2e08-4f28-b904-4a580da5b632")),
      "Series3",
      List(
        Episode("EpI", ProductionYear(2008), 1),
        Episode("EpII", ProductionYear(2005), 2))
    )
  )
  private val exampleMovie: Movie = exampleMovies.head
  private val newExampleMovie: NewMovie = NewStandalone("Movie1", ProductionYear(2007))

  private val moviesEndpoint: PartialServerEndpoint[UsernamePassword, User, Unit, ApiError.Unauthorized.type, Unit, Any, F] = endpoint
    .tag("Movie actions")
    .in("movies")
    .securityIn(auth.basic[UsernamePassword]())
    .errorOut(statusCode(StatusCode.Forbidden).description("Unauthorized").and(stringBody.map(_ => ApiError.Unauthorized)(_.info)))
    .serverSecurityLogic { credentials =>
      for
        maybeUser <- usersService.getUser(UserName(credentials.username), credentials.password.getOrElse(""))
      yield maybeUser.toRight(ApiError.Unauthorized)
    }

  private val moviesListing = moviesEndpoint
    .get
    .out(jsonBody[List[Movie]].example(exampleMovies))
    .serverLogicSuccess(user => _ => moviesService.getMoviesForUser(user.id))

  private val getMovieById = moviesEndpoint
    .get
    .in(path[UUID].name("id"))
    .out(jsonBody[Movie].example(exampleMovie))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Movie not found").and(stringBody.map(_ => ApiError.MovieNotFound)(_.info))))
    .serverLogic(user => id => moviesService.getMovie(MovieId(id), user.id).map(_.toRight(ApiError.MovieNotFound)))

  private val createMovie = moviesEndpoint
    .post
    .in("create")
    .in(jsonBody[NewMovie].description("A movie to add").example(newExampleMovie))
    .out(statusCode(StatusCode.Created).description("Successfully created").and(jsonBody[Movie].example(exampleMovie)))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.BadRequest).description("Error when creating a movie").and(stringBody.map(ApiError.InvalidData.apply)(_.info))))
    .serverLogic(user => newMovie => moviesService.createMovie(newMovie, user.id))

  private val deleteMovieById = moviesEndpoint
    .delete
    .in(path[UUID].name("id"))
    .out(statusCode(StatusCode.NoContent).description("Successfully deleted"))
    .errorOutVariant(oneOfVariant(statusCode(StatusCode.NotFound).description("Movie not found").and(stringBody.map(_ => ApiError.MovieNotFound)(_.info))))
    .serverLogic(user => id => moviesService.deleteMovie(MovieId(id), user.id).map(_.toRight(ApiError.MovieNotFound)))

  private val updateMovieById = moviesEndpoint
    .put
    .in(path[UUID].name("id"))
    .in(jsonBody[Movie].example(exampleMovie))
    .out(statusCode(StatusCode.NoContent).description("Successfully updated as instructed (no need to fetch)"))
    .errorOutVariants[ApiErrorInfo](
        oneOfVariant(StatusCode.NotFound, stringBody.description("Not found").map(_ => ApiError.MovieNotFound)(_.info)),
        oneOfVariant(StatusCode.BadRequest, stringBody.description("Id mismatch or other error").map(_ => ApiError.IdMismatch)(_.info)),
        oneOfDefaultVariant(stringBody.map(ApiError.InvalidData.apply)(_.info))
    )
    .serverLogic { user => (id, updatedMovie) =>
      moviesService.updateMovie(MovieId(id), updatedMovie, user.id)
    }

  val apiEndpoints: List[ServerEndpoint[Any, F]] = List(
    moviesListing,
    getMovieById,
    createMovie,
    deleteMovieById,
    updateMovieById
  )

  val docEndpoints: List[ServerEndpoint[Any, F]] = SwaggerInterpreter()
    .fromServerEndpoints[F](apiEndpoints, "Movies DB", "1.0.0")

  val all: List[ServerEndpoint[Any, F]] = apiEndpoints ++ docEndpoints
