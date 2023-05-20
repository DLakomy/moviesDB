package moviesdb

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import doobie.implicits.*
import doobie.{ConnectionIO, DataSourceTransactor, ExecutionContexts, Transactor}
import moviesdb.domain.*
import moviesdb.sqliteSupport.Utils.*
import moviesdb.users.UsersRepo

class UsersRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  val ds = dataSourceFromConnString(inMemoryConnString)
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit = initDb[IO](ds).unsafeRunSync()

  private val usersRepo = UsersRepo(transactor)

  test("The query should typecheck") {
    check(UsersRepo.getUserQry(UserName("aqq"), PasswordHash("123")))
  }

  test("Should retrieve user by name and password hash") {

    val username = "aqq"
    val pwdHash = "123"

    val insert: ConnectionIO[UserId] = for {
      _ <- sql"insert into users(name, password_hash) values ($username, $pwdHash)".update.run
      id <- sql"select id from users where rowid = last_insert_rowid()".query[UserId].unique
    } yield id

    val id = insert.transact(transactor).unsafeRunSync()

    val expectedUser: User = User(id, UserName(username))

    val maybeUser =
      usersRepo
        .getUser(UserName(username), PasswordHash(pwdHash))
        .unsafeRunSync()

    assertEquals(maybeUser, Some(expectedUser))
  }
