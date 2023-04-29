package moviesdb.movies

import moviesdb.domain.*
import moviesdb.domain.Movies.*

class MoviesRepo[F[_]] extends MoviesRepoAlgebra[F]:
  def getMoviesForUser(id: UserId): F[List[Movie]] = ???
  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] = ???
  def createMovie(movie: NewMovie, userId: UserId): F[ErrorOr[Movie]] = ???
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] = ???
  def updateMovie(updatedMovie: Movie, userId: UserId): F[ErrorOr[Unit]] = ???
