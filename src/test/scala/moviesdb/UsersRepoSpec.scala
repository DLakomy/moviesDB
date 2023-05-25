package moviesdb.users

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import doobie.implicits.*
import doobie.{munit as _, *}
import moviesdb.domain.*
import moviesdb.sqliteSupport.Utils.*
import moviesdb.users.UsersRepo

import java.util.UUID

class UsersRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  val ds = dataSourceFromConnString(inMemoryConnString)
  val transactor: Transactor[IO] = Transactor.fromConnection(ds.getConnection)

  override def beforeAll(): Unit = initDb[IO](ds).unsafeRunSync()

  private val usersRepo = UsersRepo(transactor)

  test("The query should typecheck") {
    check(UsersRepo.getUserQry(UserName("aqq"), PasswordHash("123")))
  }

  test("Should retrieve user by name and password hash") {

    val id = UserId(UUID.fromString("aca992e6-fb27-11ed-be56-0242ac120002"))
    val username = "aqq"
    val pwdHash = "123"

    // insert example record
    sql"insert into users(id, name, password_hash) values ($id, $username, $pwdHash)"
      .update
      .run
      .transact(transactor).unsafeRunSync()

    val expectedUser: User = User(id, UserName(username))

    val maybeUser =
      usersRepo
        .getUser(UserName(username), PasswordHash(pwdHash))
        .unsafeRunSync()

    assertEquals(maybeUser, Some(expectedUser))
  }
