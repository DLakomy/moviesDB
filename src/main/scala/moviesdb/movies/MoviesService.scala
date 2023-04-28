package moviesdb.movies
import cats.Monad
import moviesdb.domain.*
import moviesdb.domain.Movies.*

class MoviesService[F[_]](using F: Monad[F]) extends MoviesServiceAlgebra[F]:
  def getMoviesForUser(id: UserId): F[List[Movie]] = ???
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] = ???
  def createMovie(movie: NewMovie, userId: UserId): F[Either[ErrorInfo, Movie]] = ???
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] = ???
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[ErrorInfo, Unit]] =
    val id: Int = movieId.id
    F.pure(
      if id > 10 then Left(ApiError.MovieNotFound)
      else if id == 6 then Left(ApiError.InvalidData("The data is invalid"))
      else if id > 5 then Left(ApiError.IdMismatch)
      else Right(())
    )
