package moviesdb

import cats.effect.{IO, Resource}
import doobie.implicits.*
import doobie.{ConnectionIO, DataSourceTransactor, ExecutionContexts, Transactor}
import moviesdb.domain.*
import moviesdb.users.UsersRepo
import org.sqlite.SQLiteDataSource
import cats.effect.unsafe.implicits.global

import javax.sql.DataSource

class UsersRepoSpec extends munit.FunSuite with doobie.munit.IOChecker:

  // TODO in memory + flyway
  val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.sqlite.JDBC", "jdbc:sqlite:movies.db?foreign_keys=on;", "", ""
  )

  private val usersRepo = UsersRepo(transactor)

  test("The query should typecheck") {
    check(UsersRepo.getUserQry(UserName("aqq"), PasswordHash("123")))
  }

  test("Should retrieve user by name and password hash") {

    val insert: ConnectionIO[UserId] = for {
      _ <- sql"insert into users(name, password_hash) values ('aqq', '123');".update.run
      id <- sql"select id from users where rowid = last_insert_rowid()".query[UserId].unique
    } yield id

    val id = insert.transact(transactor).unsafeRunSync()

    val maybeUserId =
      usersRepo
        .getUser(UserName("aqq"), PasswordHash("123"))
        .unsafeRunSync()
        .map(_.id)

    assertEquals(Some(id), maybeUserId)
  }
