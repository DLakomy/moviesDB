package moviesdb.movies.sqlite

import moviesdb.domain.*
import moviesdb.domain.Movies.{*, given}
import cats.effect.IO
import cats.effect.kernel.MonadCancelThrow
import cats.effect.std.UUIDGen
import doobie.implicits.*
import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.{ConnectionIO, Read, Transactor, Update}
import moviesdb.domain.{PasswordHash, User, UserName}
import moviesdb.movies.{DbErrorOr, MoviesRepoAlgebra}

import java.util.UUID

class MoviesRepo[F[_]: MonadCancelThrow: UUIDGen](xa: Transactor[F]) extends MoviesRepoAlgebra[F]:
  import MoviesQueries.*

  def getMoviesForUser(id: UserId): F[List[Movie]] =
    val transaction = for
      standalones <- getStandalonesForUserQry(id).to[List]
      series <- getSeriesForUserQry(id).to[List]
    yield standalones ++ series

    transaction.transact(xa)

  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] = ???
  def createMovie(movie: NewMovie, userId: UserId): F[DbErrorOr[Movie]] = ???
  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] = ???
  def updateMovie(updatedMovie: Movie, userId: UserId): F[DbErrorOr[Unit]] = ???

private[this] object MoviesQueries:
  def getStandalonesForUserQry(userId: UserId): Query0[Standalone] =
    sql"SELECT id, title, year FROM standalones WHERE owner_id = $userId"
      .query[Standalone]

  def getSeriesForUserQry(userId: UserId): Query0[Series] = ???

  def insertMovie(movie: Standalone, userId: UserId): Update0 =
    sql"""
      INSERT INTO standalones (id, title, year, owner_id)
      VALUES (${movie.id}, ${movie.title}, ${movie.year}, $userId)
    """.update
