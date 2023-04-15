package moviesdb.movies
import moviesdb.domain.*
import moviesdb.domain.Movies.*

class MoviesServiceLive[F[_]] extends MoviesServiceAlgebra[F]:
  def getMoviesForUser(id: UserId): F[List[Movie]] = ???
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] = ???
  def createMovie(movie: NewMovie, userId: UserId): F[ErrorOr[Movie]] = ???
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] = ???
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[UpdateError, Unit]] = ???
