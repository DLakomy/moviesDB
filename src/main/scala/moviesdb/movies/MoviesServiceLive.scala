package moviesdb.movies
import cats.Monad
import moviesdb.domain.*
import moviesdb.domain.Movies.*

class MoviesServiceLive[F[_]](using F: Monad[F]) extends MoviesServiceAlgebra[F]:
  def getMoviesForUser(id: UserId): F[List[Movie]] = ???
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] = ???
  def createMovie(movie: NewMovie, userId: UserId): F[ErrorOr[Movie]] = ???
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] = ???
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[UpdateError, Unit]] =
    val id: Int = movieId.id
    F.pure(
      if id > 10 then Left(UpdateError.NotFound)
      else if id > 5 then Left(UpdateError.IdMismatch)
      else Right(())
    )
