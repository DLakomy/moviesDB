package moviesdb.movies

import moviesdb.domain.Movies.{Movie, MovieId, NewMovie}
import moviesdb.domain.{DbErrorInfo, UserId}

type DbErrorOr[A] = Either[DbErrorInfo, A]

trait MoviesRepoAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  def createMovie(movie: NewMovie, userId: UserId): F[DbErrorOr[Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(updatedMovie: Movie, userId: UserId): F[DbErrorOr[Unit]]
