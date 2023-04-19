package moviesdb.movies

import moviesdb.domain.Movies.{Movie, MovieId, NewMovie}
import moviesdb.domain.{ErrorInfo, UserId}

trait MoviesServiceAlgebra[F[_]]:
  def getMoviesForUser(id: UserId): F[List[Movie]]
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]]
  def createMovie(movie: NewMovie, userId: UserId): F[Either[ErrorInfo, Movie]]
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]]
  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[ErrorInfo, Unit]]
