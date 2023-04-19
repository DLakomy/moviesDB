package moviesdb.movies

import moviesdb.domain.Movies.{Movie, MovieId, NewMovie}
import moviesdb.domain.{ErrorInfo, UserId}

type ErrorOr[A] = Either[String, A]

trait MoviesRepoAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  def createMovie(movie: NewMovie, userId: UserId): F[ErrorOr[Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(updatedMovie: Movie, userId: UserId): F[ErrorOr[Unit]]
