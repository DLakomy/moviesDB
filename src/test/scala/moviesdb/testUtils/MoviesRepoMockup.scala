package moviesdb.testUtils

import cats.Applicative
import cats.syntax.all.*
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle
import moviesdb.domain.*
import moviesdb.domain.Movies.*
import moviesdb.movies.{DbErrorOr, MoviesRepoAlgebra}

case class DbRow(userId: UserId, movie: Movie)

type DbState = Vector[DbRow]

// I chose a plain var instead of eg. Ref[IO, List[Movie]], since it's gonna be used in one thread
// I thought the overhead is not justified (it's easy to refactor if needed, tho)
class MoviesRepoMockup[F[_]: Applicative](private var _state: DbState) extends MoviesRepoAlgebra[F]:

  def state: DbState = _state

  assert(
    _state
      .groupBy { case DbRow(_, mov) => mov.id }
      .forall { (_, v) => v.length <= 1 },
    "Ids in the state must be unique"
  )

  // again no Ref, same reasoning
  object idSeq:
    private var currVal: Int =
      _state.map{ case DbRow(_, mov) => mov.id.id }.maxOption.getOrElse(0)

    def nextVal(): MovieId =
      currVal += 1
      MovieId(currVal)

  def getMoviesForUser(id: UserId): F[List[Movie]] =
    _state.view.filter(_.userId == id).map(_.movie).toList.pure[F]

  def getMovie(movieId: MovieId, userId: UserId): F[Option[Movie]] =
    _state.find(row => row.movie.id == movieId && row.userId == userId).map(_.movie).pure[F]

  def createMovie(movie: NewMovie, userId: UserId): F[DbErrorOr[Movie]] =
    val movieWithId = movie.withId(idSeq.nextVal())
    _state = DbRow(userId, movieWithId) +: _state
    (Right(movieWithId): DbErrorOr[Movie]).pure[F]

  def deleteMovie(movieId: MovieId, userId: UserId): F[Option[Unit]] =
    val idx = _state.indexWhere(row => row.userId == userId && row.movie.id == movieId)
    if (idx < 0) None.pure[F]
    else {
      _state = _state.filterNot(_.movie.id == movieId)
      Some(()).pure[F]
    }

  def updateMovie(updatedMovie: Movie, userId: UserId): F[DbErrorOr[Unit]] =
    val idx = _state.indexWhere(row => row.userId == userId && row.movie.id == updatedMovie.id)
    if (idx < 0) Left(DbError.MovieNotFound).pure[F]
    else {
      _state = _state.updated(idx, DbRow(userId, updatedMovie))
      Right(()).pure[F]
    }