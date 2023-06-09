package moviesdb.movies.sqlite

import moviesdb.domain.*
import moviesdb.domain.Movies.{*, given}
import cats.effect.IO
import cats.effect.kernel.MonadCancelThrow
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import doobie.implicits.*
import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.{ConnectionIO, Read, Transactor, Update}
import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.DbError.InvalidData
import moviesdb.domain.{PasswordHash, User, UserName}
import moviesdb.movies.{DbErrorOr, MoviesRepoAlgebra}

import java.util.UUID

class MoviesRepo[F[_]: MonadCancelThrow: UUIDGen](xa: Transactor[F]) extends MoviesRepoAlgebra[F]:
  import MoviesQueries.*

  def getMoviesForUser(id: UserId): F[List[Movie]] =
    val transaction = for
      standalones <- getStandalonesForUserQry(id).to[List]
      series <- getAllSeriesForUserQry(id).to[List]
    yield standalones ++ series

    transaction.transact(xa)

  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] =
    getStandaloneForUserQry(movieId, userId).option.transact(xa).flatMap {
      case None =>
        val query = for
          maybeHeader <- getSeriesHeaderForUserQry(movieId, userId).option
          fetchedEps <-
            if (maybeHeader.isEmpty) List.empty[Episode].pure[ConnectionIO]
            else getEpisodesForSeriesQry(movieId).to[List]
        yield maybeHeader.map(_.copy(episodes = fetchedEps))

        query.transact(xa).widen

      case a@Some(_) => a.pure
    }

  def createMovie(movie: NewMovie, userId: UserId): F[DbErrorOr[Movie]] = movie match
    case standalone: NewStandalone =>
      for
        newId <- UUIDGen.randomUUID.map(MovieId.apply)
        newStandalone = standalone.withId(newId)
        _ <- insertStandaloneQry(newStandalone, userId).run.transact(xa)
      yield Right(newStandalone) // no expected errors
    case series: NewSeries =>
      for
        newId <- UUIDGen.randomUUID.map(MovieId.apply)
        newSeries = series.withId(newId)
        _ <- (
          insertSeriesQry(newSeries, userId).run >>
          newSeries.episodes.traverse(ep => insertEpisodeQry(newId, ep).run)
        ).transact(xa)
      yield Right(newSeries)

  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] =
    deleteStandaloneQry(movieId, userId).run.transact(xa).flatMap { n =>
      if (n == 0 )
        (deleteEpisodesQry(movieId, userId).run >> deleteSeriesQry(movieId, userId).run)
          .transact(xa).map(n => Option.when(n>0)(()))
      else
        Some(()).pure
    }

  def updateMovie(updatedMovie: Movie, userId: UserId): F[DbErrorOr[Unit]] = updatedMovie match
    case updatedStandalone: Standalone =>
      updateStandaloneQry(updatedStandalone, userId).run.transact(xa)
        .map(n => Either.cond(n>0, (), DbError.MovieNotFound))
    case _ => ???


private[this] object MoviesQueries:
  def getStandalonesForUserQry(userId: UserId): Query0[Standalone] =
    sql"SELECT id, title, year FROM standalones WHERE owner_id = $userId"
      .query[Standalone]

  def getStandaloneForUserQry(movieId: MovieId, userId: UserId): Query0[Standalone] =
    sql"SELECT id, title, year FROM standalones WHERE owner_id = $userId AND id = $movieId"
      .query[Standalone]

  def getAllSeriesForUserQry(userId: UserId): Query0[Series] = ???

  // the episodes should be fetched separately (it is a separate query, so I can check it in UT)
  def getSeriesHeaderForUserQry(movieId: MovieId, userId: UserId): Query0[Series] =
    sql"SELECT id, title FROM series WHERE owner_id = $userId AND id = $movieId"
      .query[(MovieId, String)].map((id, title) => Series(id, title, List.empty))

  def insertStandaloneQry(movie: Standalone, userId: UserId): Update0 =
    sql"""
      INSERT INTO standalones (id, title, year, owner_id)
      VALUES (${movie.id}, ${movie.title}, ${movie.year}, $userId)
    """.update

  def insertSeriesQry(movie: Series, userId: UserId): Update0 =
    sql"""
      INSERT INTO series (id, title, owner_id)
      VALUES (${movie.id}, ${movie.title}, $userId)
    """.update

  // BTW it would be possible to use Update[...](sql).updateMany(ps) after some refactoring
  // can be risky tho: https://github.com/tpolecat/doobie/issues/706
  def insertEpisodeQry(seriesId: MovieId, episode: Episode): Update0 =
    sql"""
      INSERT INTO episodes (series_id, title, year, number)
      VALUES ($seriesId, ${episode.title}, ${episode.year}, ${episode.number})
    """.update

  def getEpisodesForSeriesQry(seriesId: MovieId): Query0[Episode] =
    sql"""
      SELECT title, year, number FROM episodes WHERE series_id = $seriesId
       """.query

  def deleteStandaloneQry(movieId: MovieId, userId: UserId): Update0 =
    sql"""
         DELETE FROM standalones WHERE id = $movieId AND owner_id = $userId
       """.update

  def updateStandaloneQry(updatedMovie: Standalone, userId: UserId): Update0 =
    sql"""
         UPDATE standalones
            SET title = ${updatedMovie.title}
              , year = ${updatedMovie.year}
          WHERE id = ${updatedMovie.id} AND owner_id = $userId
       """.update

  def deleteEpisodesQry(movieId: MovieId, userId: UserId): Update0 =
    sql"""
         DELETE FROM episodes
          WHERE series_id = (
                  SELECT id
                    FROM series
                   WHERE id = $movieId
                     AND owner_id = $userId
                )
       """.update

  def deleteSeriesQry(movieId: MovieId, userId: UserId): Update0 =
    sql"""
         DELETE FROM series
          WHERE id = $movieId
            AND owner_id = $userId
       """.update
