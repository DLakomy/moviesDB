package moviesdb.users

import cats.effect.IO
import cats.effect.kernel.MonadCancelThrow
import doobie.implicits.*
import doobie.util.query.Query0
import doobie.{ConnectionIO, Read, Transactor}
import moviesdb.domain.{PasswordHash, User, UserName}

class UsersRepo[F[_]: MonadCancelThrow](xa: Transactor[F]) extends UsersRepoAlgebra[F]:
  import UsersRepo.*
  def getUser(userName: UserName, passwordHash: PasswordHash): F[Option[User]] =
    getUserQry(userName, passwordHash)
      .option
      .transact(xa)

// private so the queries are not visible outside
private[this] object UsersRepo:
  def getUserQry(userName: UserName, passwordHash: PasswordHash): Query0[User] =
    sql"SELECT id, name FROM users WHERE name = $userName AND password_hash = $passwordHash"
      .query[User]
