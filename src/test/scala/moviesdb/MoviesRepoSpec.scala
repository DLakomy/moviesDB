package moviesdb.movies.sqlite

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import doobie.implicits.*
import doobie.{munit as _, *}
import moviesdb.core.syntax.MoviesSyntax.*
import moviesdb.domain.*
import moviesdb.domain.Movies.Standalone
import moviesdb.movies.sqlite.MoviesRepo
import moviesdb.sqliteSupport.Utils.*
import moviesdb.testUtils.*

import java.util.UUID

class MoviesRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  private val uid = UserId(UUID.randomUUID())
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

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
    // check(insertMovie(standaloneTemplate.withId(mid1), uid1))
  }

  test("Should create and retrieve a standalone movie") {

    val program = for
      maybeIdFromCreate <- moviesRepo.createMovie(standaloneTemplate, uid).map(_.map(_.id))
      id <- maybeIdFromCreate match {
        case Left(err) => IO(fail("Movie creation failed: " + err.toString))
        case Right(id) => id.pure
      }
      movieFromRepo <- moviesRepo.getMovie(id, uid)
      otherUserMovie <- moviesRepo.getMovie(id, otherUid)
    yield (id, movieFromRepo, otherUserMovie)

    val (newId, movieFromRepo, otherUserMovie) = program.unsafeRunSync()

    assertEquals(movieFromRepo, Some(standaloneTemplate.withId(newId)), "Movie not found")
    assertEquals(otherUserMovie, None, "Movie shouldn't be found with a wrong userId")
  }
