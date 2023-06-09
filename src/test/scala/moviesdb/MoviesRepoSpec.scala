package moviesdb.movies.sqlite

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import doobie.implicits.*
import doobie.{munit as _, *}
import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.*
import moviesdb.domain.Movies.{Episode, Movie, MovieId, NewMovie, NewStandalone, ProductionYear}
import moviesdb.movies.sqlite.MoviesRepo
import moviesdb.sqliteSupport.Utils.*
import moviesdb.testUtils.*

import java.util.UUID

// TODO comparing episodes lists can be flaky, the order from db is not guaranteed to be the same
// it's worth to write a proper Compare given
// TODO and better use random user ids...

class MoviesRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  // to be used in the tests acting on a single movie
  // (not when getting all movies for the user)
  // (maybe shoulda generate another one for every test ¯\_(ツ)_/¯ )
  private val uid1 = UserId(UUID.randomUUID())
  private val uid2 = UserId(UUID.randomUUID())
  // to be used when checking the behaviour with a wrong user id
  // (better don't add any movie to this user)
  private val otherUid = UserId(UUID.randomUUID())

  val ds = dataSourceFromConnString(inMemoryConnString("moviesRepo"))
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit =
    initDb[IO](ds).unsafeRunSync()
    ( sql"insert into users(id, name, password_hash) values ($uid1, 'testuser', 'whatever')".update.run >>
      sql"insert into users(id, name, password_hash) values ($uid2, 'testuser2', 'whatever')".update.run )
      .transact(transactor).unsafeRunSync()

  private val moviesRepo = MoviesRepo(transactor)

  test("The DMLs should typecheck") {
    import MoviesQueries.*

    val randomId = MovieId(UUID.randomUUID())
    val standalone = standaloneTemplate.withId(randomId)
    val series = newSeries1.withId(randomId)

    check(getStandalonesForUserQry(uid1))
    check(getStandaloneForUserQry(mid1, uid1))
    check(deleteStandaloneQry(mid1, uid1))

    check(insertSeriesQry(series, uid1))
    check(getEpisodesForSeriesQry(mid1))
    check(deleteEpisodesQry(mid1, uid1))
    check(deleteSeriesQry(mid1, uid1))
  }

  test("Some failing DMLs should typecheck after Doobie issue #1782 is fixed".ignore) {
    import MoviesQueries.*

    val randomId = MovieId(UUID.randomUUID())
    val standalone = standaloneTemplate.withId(randomId)
    val series = newSeries1.withId(randomId)

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
    check(insertStandaloneQry(standaloneTemplate.withId(mid1), uid1))
    check(updateStandaloneQry(standalone, uid1))
    check(insertEpisodeQry(mid1, newSeries1.episodes.head))
    check(updateSeriesHeadQry(series, uid1))
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
  test("Should create and retrieve a standalone movie") {

    val program = for
      movieFromCreate <- addTestMovie(standaloneTemplate, uid1)
      id = movieFromCreate.id
      movieFromGet <- moviesRepo.getMovie(id, uid1)
      otherUserMovie <- moviesRepo.getMovie(id, otherUid)
    yield (id, movieFromGet, otherUserMovie, movieFromCreate)

    val (id, movieFromGet, otherUserMovie, movieFromCreate) = program.unsafeRunSync()

    assertEquals(movieFromCreate, standaloneTemplate.withId(id), "The movie is not identical to what was provided")
    assertEquals(Some(movieFromCreate), movieFromGet, "Movie from create doesn't match the one fetched")
    assertEquals(otherUserMovie, None, "Movie shouldn't be found with a wrong userId")
  }

  test("Should update a standalone movie") {

    val newMovie = standaloneTemplate
    val updatedMovie = // we'll add id later
      val newMovieYear = newMovie.year.year // quite unwieldy, I'll write a better domain next time
      standaloneTemplate.copy(title = newMovie.title + "a", year = ProductionYear(newMovieYear + 1))

    val program = for
      movieFromCreate <- addTestMovie(newMovie, uid1)
      id = movieFromCreate.id
      correctUpdateResult <- moviesRepo.updateMovie(updatedMovie.withId(id), uid1)
      failedUpdateResult <- moviesRepo.updateMovie(newMovie.withId(id), otherUid)
      movieFromRepo <- moviesRepo.getMovie(id, uid1)
    yield (id, correctUpdateResult, failedUpdateResult, movieFromRepo)

    val (id, correctUpdateResult, failedUpdateResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(movieFromRepo, Some(updatedMovie.withId(id)), "Not updated correctly")
    assertEquals(failedUpdateResult, Left(DbError.MovieNotFound), "Movie shouldn't be found with a wrong userId")
  }

  test("Should delete a standalone movie") {
    val newMovie = standaloneTemplate

    val program = for
      movieFromCreate <- addTestMovie(newMovie, uid1)
      id = movieFromCreate.id
      deleteOtherUserResult <- moviesRepo.deleteMovie(id, otherUid)
      deleteCorrectResult <- moviesRepo.deleteMovie(id, uid1)
      deleteAgainResult <- moviesRepo.deleteMovie(id, uid1)
      movieFromRepo <- moviesRepo.getMovie(id, uid1)
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
  test("Should create and retrieve a series") {

    val program = for
      movieFromCreate <- addTestMovie(newSeries1, uid1)
      // a second one to be sure, that the join between series and episodes is correct
      _ <- addTestMovie(newSeries2, uid1)
      id = movieFromCreate.id
      movieFromGet <- moviesRepo.getMovie(id, uid1)
      otherUserMovie <- moviesRepo.getMovie(id, otherUid)
    yield (id, movieFromGet, otherUserMovie, movieFromCreate)

    val (id, movieFromGet, otherUserMovie, movieFromCreate) = program.unsafeRunSync()

    assertEquals(movieFromCreate, newSeries1.withId(id), "The movie is not identical to what was provided")
    assertEquals(Some(movieFromCreate), movieFromGet, "Movie from create doesn't match the one fetched")
    assertEquals(otherUserMovie, None, "Movie shouldn't be found with a wrong userId")
  }

  test("Should delete a series") {
    val newMovie = standaloneTemplate

    val program = for
      movieFromCreate <- addTestMovie(newSeries1, uid1)
      id = movieFromCreate.id
      deleteOtherUserResult <- moviesRepo.deleteMovie(id, otherUid)
      // to be surce that the episodes remain untouched
      movieFromRepoUntouched <- moviesRepo.getMovie(id, uid1)
      deleteCorrectResult <- moviesRepo.deleteMovie(id, uid1)
      deleteAgainResult <- moviesRepo.deleteMovie(id, uid1)
      movieFromRepo <- moviesRepo.getMovie(id, uid1)
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
    assertEquals(movieFromRepoUntouched, Some(movieFromCreate), "Shouldn't touch a series if it's not being deleted")
    assertEquals(deleteCorrectResult, Some(()))
    assertEquals(deleteAgainResult, None, "The movie shouldn't exist at this point")
    assertEquals(movieFromRepo, None, "Movie shouldn't be found after deletion")
  }

  test("Should update a series") {

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
      movieFromCreate <- addTestMovie(newMovie, uid1)
      id = movieFromCreate.id
      correctUpdateResult <- moviesRepo.updateMovie(updatedMovie.withId(id), uid1)
      failedUpdateResult <- moviesRepo.updateMovie(newMovie.withId(id), otherUid)
      movieFromRepo <- moviesRepo.getMovie(id, uid1)
    yield (id, correctUpdateResult, failedUpdateResult, movieFromRepo)

    val (id, correctUpdateResult, failedUpdateResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(movieFromRepo, Some(updatedMovie.withId(id)), "Not updated correctly")
    assertEquals(failedUpdateResult, Left(DbError.MovieNotFound), "Movie shouldn't be found with a wrong userId")
  }

  test("Should retrieve all movies for a given user") {

    val user1Movies: List[NewMovie] = List(standaloneTemplate, newSeries1, newSeries2)

    val user2Movies: List[NewMovie] = List(
      standaloneTemplate.copy(title = standaloneTemplate.title+" second"),
      newSeries1.copy(episodes = newSeries1.episodes.map(ep => ep.copy(title = ep.title+" second"))),
      newSeries2.copy(episodes = newSeries1.episodes.map(ep => ep.copy(title = ep.title+" second")))
    )

    val program = for
      u1MoviesCreated <- user1Movies.traverse(mv => addTestMovie(mv, uid1))
      u2MoviesCreated <- user2Movies.traverse(mv => addTestMovie(mv, uid2))
      u1MoviesGot <- moviesRepo.getMoviesForUser(uid1)
      u2MoviesGot <- moviesRepo.getMoviesForUser(uid2)
    yield (u1MoviesCreated, u1MoviesGot, u2MoviesCreated, u2MoviesGot)

    val (u1MoviesCreated, u1MoviesGot, u2MoviesCreated, u2MoviesGot) = program.unsafeRunSync()

    assertEquals(u1MoviesGot, u1MoviesCreated)
    assertEquals(u2MoviesGot, u2MoviesCreated)
}
