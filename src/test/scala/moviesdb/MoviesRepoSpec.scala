package moviesdb.movies.sqlite

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import doobie.implicits.*
import doobie.{munit as _, *}
import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.*
import moviesdb.domain.Movies.{Movie, NewMovie, NewStandalone, ProductionYear}
import moviesdb.movies.sqlite.MoviesRepo
import moviesdb.sqliteSupport.Utils.*
import moviesdb.testUtils.*

import java.util.UUID

class MoviesRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  // to be used in the tests acting on a single movie
  // (not when getting all movies for the user)
  // (maybe shoulda generate another one for every test ¯\_(ツ)_/¯ )
  private val uid = UserId(UUID.randomUUID())
  // to be used when checking the behaviour with a wrong user id
  // (better don't add any movie to this user)
  private val otherUid = UserId(UUID.randomUUID())

  val ds = dataSourceFromConnString(inMemoryConnString("moviesRepo"))
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit =
    initDb[IO](ds).unsafeRunSync()
    sql"insert into users(id, name, password_hash) values ($uid, 'testuser', 'whatever')"
      .update.run.transact(transactor).unsafeRunSync()

  private val moviesRepo = MoviesRepo(transactor)

  test("The DMLs should typecheck") {
    import MoviesQueries.*

    val randomUUID = UUID.randomUUID()

    check(getStandalonesForUserQry(uid))
    check(getStandaloneForUserQry(mid1, uid))
    check(deleteStandaloneQry(mid1, uid))

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
    // check(insertMovie(standaloneTemplate.withId(mid1), uid1))
  }

  test("The DMLs should typecheck after Doobie issue #1782 is fixed".ignore) {
    import MoviesQueries.*

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
     check(insertStandaloneQry(standaloneTemplate.withId(mid1), uid1))
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
      movieFromCreate <- addTestMovie(standaloneTemplate, uid)
      id = movieFromCreate.id
      movieFromGet <- moviesRepo.getMovie(id, uid)
      otherUserMovie <- moviesRepo.getMovie(id, otherUid)
    yield (movieFromGet, otherUserMovie, movieFromCreate)

    val  (movieFromGet, otherUserMovie, movieFromCreate) = program.unsafeRunSync()

    assertEquals(Some(movieFromCreate), movieFromGet, "Movie not found")
    assertEquals(otherUserMovie, None, "Movie shouldn't be found with a wrong userId")
  }


  test("Should update a standalone movie") {

    val newMovie = standaloneTemplate
    val updatedMovie = // we'll add id later
      val newMovieYear = newMovie.year.year // quite unwieldy, I'll write a better domain next time
      standaloneTemplate.copy(title = newMovie.title + "a", year = ProductionYear(newMovieYear + 1))

    val program = for
      movieFromCreate <- addTestMovie(newMovie, uid)
      id = movieFromCreate.id
      correctUpdateResult <- moviesRepo.updateMovie(updatedMovie.withId(id), uid)
      failedUpdateResult <- moviesRepo.updateMovie(newMovie.withId(id), otherUid)
      movieFromRepo <- moviesRepo.getMovie(id, uid)
    yield (id, correctUpdateResult, failedUpdateResult, movieFromRepo)

    val (id, correctUpdateResult, failedUpdateResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(movieFromRepo, Some(updatedMovie.withId(id)), "Not updated correctly")
    assertEquals(failedUpdateResult, Left(DbError.MovieNotFound), "Movie shouldn't be found with a wrong userId")
  }

  test("Should delete a standalone movie") {
    val newMovie = standaloneTemplate

    val program = for
      movieFromCreate <- addTestMovie(newMovie, uid)
      id = movieFromCreate.id
      deleteOtherUserResult <- moviesRepo.deleteMovie(id, otherUid)
      deleteCorrectResult <- moviesRepo.deleteMovie(id, uid)
      deleteAgainResult <- moviesRepo.deleteMovie(id, uid)
      movieFromRepo <- moviesRepo.getMovie(id, uid)
    yield (deleteOtherUserResult, deleteCorrectResult, deleteAgainResult, movieFromRepo)

    val (deleteOtherUserResult, deleteCorrectResult, deleteAgainResult, movieFromRepo) = program.unsafeRunSync()

    assertEquals(deleteOtherUserResult, None, "Shouldn't delete other user's movie")
    assertEquals(deleteCorrectResult, Some(()))
    assertEquals(deleteAgainResult, None, "The movie shouldn't exist at this point")
    assertEquals(movieFromRepo, None, "Movie shouldn't be found after deletion")
  }
