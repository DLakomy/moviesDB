package moviesdb.movies.sqlite

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import doobie.implicits.*
import doobie.{munit as _, *}
import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.{UserId, *}
import moviesdb.domain.Movies.{Episode, Movie, MovieId, NewMovie, NewStandalone, ProductionYear}
import moviesdb.movies.sqlite.MoviesRepo
import moviesdb.sqliteSupport.Utils.*
import moviesdb.testUtils.*
import moviesdb.testUtils.syntax.MoviesSyntax.*

import java.util.UUID

class MoviesRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  // Always two, there are. No more. No less.
  private val withUsers = FunFixture[(UserId, UserId)] (
    setup = { _ =>
      val usr1 = UserId(UUID.randomUUID())
      val usr2 = UserId(UUID.randomUUID())
      (sql"insert into users(id, name, password_hash) values ($usr1, 'testuser', 'whatever')".update.run >>
        sql"insert into users(id, name, password_hash) values ($usr2, 'testuser2', 'whatever')".update.run)
        .transact(transactor).unsafeRunSync()
      (usr1, usr2)
    },
    teardown = _ => () // theoretically it shouldn't collide...
  )

  val ds = dataSourceFromConnString(inMemoryConnString("moviesRepo"))
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit =
    initDb[IO](ds).unsafeRunSync()

  private val moviesRepo = MoviesRepo(transactor)

  test("The DMLs should typecheck") {
    import MoviesQueries.*

    val randomId = MovieId(UUID.randomUUID())
    val standalone = standaloneTemplate.withId(randomId)
    val series = newSeries1.withId(randomId)
    val usr1 = UserId(UUID.randomUUID())

    check(getStandaloneForUserQry(None, usr1))
    check(getStandaloneForUserQry(Some(mid1), usr1))
    check(deleteStandaloneQry(mid1, usr1))

    check(insertSeriesQry(series, usr1))
    check(getSeriesHeaderQry(None, usr1))
    check(getSeriesHeaderQry(Some(mid1), usr1))
    check(getEpisodesForSeriesQry(mid1))
    check(getEpisodesForSeriesQry(List(mid1, mid1)))
    check(deleteEpisodesQry(mid1, usr1))
    check(deleteSeriesQry(mid1, usr1))
  }

  test("Some failing DMLs should typecheck after Doobie issue #1782 is fixed".ignore) {
    import MoviesQueries.*

    val randomId = MovieId(UUID.randomUUID())
    val standalone = standaloneTemplate.withId(randomId)
    val series = newSeries1.withId(randomId)
    val usr1 = UserId(UUID.randomUUID())

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
    check(insertStandaloneQry(standaloneTemplate.withId(mid1), usr1))
    check(updateStandaloneQry(standalone, usr1))
    check(insertEpisodeQry(mid1, newSeries1.episodes.head))
    check(updateSeriesHeadQry(series, usr1))
  }

  // fails in case of creation error
  private def addTestMovie(movie: NewMovie, userId: UserId): IO[Movie] = for
    resultOrError <- moviesRepo.createMovie(movie, userId)
    result <- resultOrError match
      case Left(err) => IO(fail("Movie creation failed: " + err.toString))
      case Right(mov) => mov.pure
  yield result

  // I test create a bit more extensively here
  // (no separate test, its hard to test it without getMovie; I don't want to duplicate getMovie logic)
  withUsers.test("Should create and retrieve a standalone movie") { (usr1, usr2) =>

    val program = for
      movieFromCreate <- addTestMovie(standaloneTemplate, usr1)
      id = movieFromCreate.id
      movieFromGet <- moviesRepo.getMovie(id, usr1)
      otherUserMovie <- moviesRepo.getMovie(id, usr2)
    yield (id, movieFromGet, otherUserMovie, movieFromCreate)

    val (id, movieFromGet, otherUserMovie, movieFromCreate) = program.unsafeRunSync()

    assertEquals(movieFromCreate, standaloneTemplate.withId(id), "The movie is not identical to what was provided")
    assertEquals(Some(movieFromCreate), movieFromGet, "Movie from create doesn't match the one fetched")
    assertEquals(otherUserMovie, None, "Movie shouldn't be found with a wrong userId")
  }

  withUsers.test("Should update a standalone movie") { (usr1, usr2) =>

    val newMovie = standaloneTemplate
    val updatedMovie = // we'll add id later
      val newMovieYear = newMovie.year.year // quite unwieldy, I'll write a better domain next time
      standaloneTemplate.copy(title = newMovie.title + "a", year = ProductionYear(newMovieYear + 1))

    val program = for
      movieFromCreate <- addTestMovie(newMovie, usr1)
      id = movieFromCreate.id
      correctUpdateResult <- moviesRepo.updateMovie(updatedMovie.withId(id), usr1)
      failedUpdateResult <- moviesRepo.updateMovie(newMovie.withId(id), usr2)
      movieFromRepo <- moviesRepo.getMovie(id, usr1)
    yield (id, correctUpdateResult, failedUpdateResult, movieFromRepo)

    val (id, correctUpdateResult, failedUpdateResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(movieFromRepo, Some(updatedMovie.withId(id)), "Not updated correctly")
    assertEquals(failedUpdateResult, Left(DbError.MovieNotFound), "Movie shouldn't be found with a wrong userId")
  }

  withUsers.test("Should delete a standalone movie") { (usr1, usr2) =>
    val newMovie = standaloneTemplate

    val program = for
      movieFromCreate <- addTestMovie(newMovie, usr1)
      id = movieFromCreate.id
      deleteOtherUserResult <- moviesRepo.deleteMovie(id, usr2)
      deleteCorrectResult <- moviesRepo.deleteMovie(id, usr1)
      deleteAgainResult <- moviesRepo.deleteMovie(id, usr1)
      movieFromRepo <- moviesRepo.getMovie(id, usr1)
    yield (deleteOtherUserResult, deleteCorrectResult, deleteAgainResult, movieFromRepo)

    val (deleteOtherUserResult, deleteCorrectResult, deleteAgainResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(deleteOtherUserResult, None, "Shouldn't delete other user's movie")
    assertEquals(deleteCorrectResult, Some(()))
    assertEquals(deleteAgainResult, None, "The movie shouldn't exist at this point")
    assertEquals(movieFromRepo, None, "Movie shouldn't be found after deletion")
  }

  // I test create a bit more extensively here
  // (no separate test, its hard to test it without getMovie; I don't want to duplicate getMovie logic)
  // quite similar to testing a standalone, but I don't want to be hasty with refactoring it to one function
  // it can possibly diverge in the hypothetical future
  withUsers.test("Should create and retrieve a series") { (usr1, usr2) =>

    val program = for
      movieFromCreate <- addTestMovie(newSeries1, usr1)
      // a second one to be sure, that the join between series and episodes is correct
      _ <- addTestMovie(newSeries2, usr1)
      id = movieFromCreate.id
      movieFromGet <- moviesRepo.getMovie(id, usr1)
      otherUserMovie <- moviesRepo.getMovie(id, usr2)
    yield (id, movieFromGet, otherUserMovie, movieFromCreate)

    val (id, movieFromGet, otherUserMovie, movieFromCreate) = program.unsafeRunSync()

    assertEquals(movieFromCreate.normalised, newSeries1.withId(id).normalised, "The movie is not identical to what was provided")
    assertEquals(Some(movieFromCreate.normalised), movieFromGet.map(_.normalised), "Movie from create doesn't match the one fetched")
    assertEquals(otherUserMovie, None, "Movie shouldn't be found with a wrong userId")
  }

  withUsers.test("Should delete a series") { (usr1, usr2) =>
    val program = for
      movieFromCreate <- addTestMovie(newSeries1, usr1)
      id = movieFromCreate.id
      deleteOtherUserResult <- moviesRepo.deleteMovie(id, usr2)
      // to be surce that the episodes remain untouched
      movieFromRepoUntouched <- moviesRepo.getMovie(id, usr1)
      deleteCorrectResult <- moviesRepo.deleteMovie(id, usr1)
      deleteAgainResult <- moviesRepo.deleteMovie(id, usr1)
      movieFromRepo <- moviesRepo.getMovie(id, usr1)
    yield (
      movieFromCreate,
      movieFromRepoUntouched,
      deleteOtherUserResult,
      deleteCorrectResult,
      deleteAgainResult,
      movieFromRepo
    )

    val (
      movieFromCreate,
      movieFromRepoUntouched,
      deleteOtherUserResult,
      deleteCorrectResult,
      deleteAgainResult,
      movieFromRepo
    ) = program.unsafeRunSync()

    assertEquals(deleteOtherUserResult, None, "Shouldn't delete other user's movie")
    // Why this test? It's quite easy to remove a detail and forget to rollback if the master can't be found
    // fot this user
    assertEquals(movieFromRepoUntouched.map(_.normalised), Some(movieFromCreate.normalised), "Shouldn't touch a series if it's not being deleted")
    assertEquals(deleteCorrectResult, Some(()))
    assertEquals(deleteAgainResult, None, "The movie shouldn't exist at this point")
    assertEquals(movieFromRepo, None, "Movie shouldn't be found after deletion")
  }

  withUsers.test("Should update a series") { (usr1, usr2) =>

    val newMovie = newSeries1
    val updatedMovie = // we'll add id later
      newMovie.copy(
        title = newMovie.title + "a",
        episodes = newMovie.episodes.map {
          // this year thing is unwieldy; I won't refactor it tho, no time :( next time
          case Episode(title, year, number) => Episode(title+'b', year.copy(year.year+1), number+1)
        }
      )

    val program = for
      movieFromCreate <- addTestMovie(newMovie, usr1)
      id = movieFromCreate.id
      correctUpdateResult <- moviesRepo.updateMovie(updatedMovie.withId(id), usr1)
      failedUpdateResult <- moviesRepo.updateMovie(newMovie.withId(id), usr2)
      movieFromRepo <- moviesRepo.getMovie(id, usr1)
    yield (id, correctUpdateResult, failedUpdateResult, movieFromRepo)

    val (id, correctUpdateResult, failedUpdateResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(movieFromRepo.map(_.normalised), Some(updatedMovie.withId(id).normalised), "Not updated correctly")
    assertEquals(failedUpdateResult, Left(DbError.MovieNotFound), "Movie shouldn't be found with a wrong userId")
  }

  withUsers.test("Should retrieve all movies for the given user") { (usr1, usr2) =>

    val user1Movies: List[NewMovie] = List(standaloneTemplate, newSeries1, newSeries2)

    val user2Movies: List[NewMovie] = List(
      standaloneTemplate.copy(title = standaloneTemplate.title+" second"),
      newSeries1.copy(episodes = newSeries1.episodes.map(ep => ep.copy(title = ep.title+" second"))),
      newSeries2.copy(episodes = newSeries1.episodes.map(ep => ep.copy(title = ep.title+" second")))
    )

    val program = for
      u1MoviesCreated <- user1Movies.traverse(mv => addTestMovie(mv, usr1)).map(_.map(_.normalised))
      u2MoviesCreated <- user2Movies.traverse(mv => addTestMovie(mv, usr2)).map(_.map(_.normalised))
      u1MoviesGot <- moviesRepo.getMoviesForUser(usr1)
      u2MoviesGot <- moviesRepo.getMoviesForUser(usr2)
    yield (u1MoviesCreated.normalised, u1MoviesGot.normalised, u2MoviesCreated.normalised, u2MoviesGot.normalised)

    val (u1MoviesCreated, u1MoviesGot, u2MoviesCreated, u2MoviesGot) = program.unsafeRunSync()

    assertEquals(u1MoviesGot, u1MoviesCreated)
    assertEquals(u2MoviesGot, u2MoviesCreated)
}
