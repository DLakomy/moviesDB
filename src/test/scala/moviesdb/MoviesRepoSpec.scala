package moviesdb.movies.sqlite

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import doobie.implicits.*
import doobie.{munit as _, *}
import moviesdb.domain.*
import moviesdb.domain.Movies.Standalone
import moviesdb.movies.sqlite.MoviesRepo
import moviesdb.sqliteSupport.Utils.*
import moviesdb.testUtils.*

import java.util.UUID

class MoviesRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  val ds = dataSourceFromConnString(inMemoryConnString("moviesRepo"))
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit = initDb[IO](ds).unsafeRunSync()

  private val moviesRepo = MoviesRepo(transactor)

  test("The DMLs should typecheck") {
    import MoviesQueries.*
    check(getStandalonesForUserQry(UserId(UUID.randomUUID())))

    // fails, I suspect https://github.com/tpolecat/doobie/issues/1782
    // check(insertMovie(standaloneTemplate.withId(mid1), uid1))
  }

  test("Should create and retrieve a movie".ignore) {
    ???
  }
