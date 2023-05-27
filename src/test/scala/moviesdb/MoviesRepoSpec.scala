package moviesdb.movies.sqlite

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
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

  val ds = dataSourceFromConnString(inMemoryConnString("moviesRepo"))
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit = initDb[IO](ds).unsafeRunSync()
  // TODO add a user

  private val moviesRepo = MoviesRepo(transactor)

  test("The DMLs should typecheck") {
    import MoviesQueries.*
    check(getStandalonesForUserQry(UserId(UUID.randomUUID())))

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
    // check(insertMovie(standaloneTemplate.withId(mid1), uid1))
  }

  test("Should create and retrieve a standalone movie") {

    val program = for
      idFromCreate <- moviesRepo.createMovie(standaloneTemplate, uid).map(_.map(_.id))
      movieFromRepo <- idFromCreate match
        case Left(err) => IO(fail("Movie creation failed: "+err.toString))
        case Right(id) =>
          moviesRepo.getMovie(id, uid)
    // get is safe below; see `fail` above
    yield (idFromCreate.toOption.get, movieFromRepo)

    val (newId, movieFromRepo) = program.unsafeRunSync()

    assertEquals(movieFromRepo, Some(standaloneTemplate.withId(newId)))
  }

  test("Should fail to retrieve a movie when user id is mismatched".ignore) {
    ???
  }
