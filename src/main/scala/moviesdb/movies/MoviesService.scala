package moviesdb.movies
import cats.Applicative
import cats.syntax.all.*
import moviesdb.domain.*
import moviesdb.domain.Movies.*

class MoviesService[F[_]: Applicative](repo: MoviesRepoAlgebra[F]) extends MoviesServiceAlgebra[F]:
  def getMoviesForUser(id: UserId): F[List[Movie]] =
    repo.getMoviesForUser(id)

  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] =
    repo.getMovie(movieId, userId)

  def createMovie(movie: NewMovie, userId: UserId): F[Either[ApiErrorInfo, Movie]] =
    repo
      .createMovie(movie, userId)
      .map(_.left.map(err => ApiError.InvalidData(err.info)))

  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] =
    repo.deleteMovie(movieId, userId)

  def updateMovie(movieId: MovieId, updatedMovie: Movie, userId: UserId): F[Either[ApiErrorInfo, Unit]] =
    if (movieId != updatedMovie.id) Left(ApiError.IdMismatch).pure[F]
    else {
      // TODO it's worth to consider using EitherT
      repo.updateMovie(updatedMovie, userId).map {
        _.left.map {
          case DbError.MovieNotFound     => ApiError.MovieNotFound
          case DbError.InvalidData(info) => ApiError.InvalidData(info)
        }
      }
    }
