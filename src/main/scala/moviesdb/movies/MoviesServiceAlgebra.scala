package moviesdb.movies

import moviesdb.domain.Movies.{Movie, MovieId, NewMovie}
import moviesdb.domain.{ApiErrorInfo, UserId}

type ApiErrorOr[A] = Either[ApiErrorInfo, A]

trait MoviesServiceAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  def createMovie(movie: NewMovie, userId: UserId): F[ApiErrorOr[Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[ApiErrorOr[Unit]]
