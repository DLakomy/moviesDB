package moviesdb.movies

import moviesdb.domain.Movies.{Movie, MovieId, NewMovie}
import moviesdb.domain.UserId

// TODO rename and move to domain
enum UpdateError:
  case IdMismatch
  case NotFound
  case Unauthorized
  case InvalidData

trait MoviesServiceAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  // TODO the error should be precisely InvalidData
  def createMovie(movie: NewMovie, userId: UserId): F[Either[UpdateError, Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[UpdateError, Unit]]
