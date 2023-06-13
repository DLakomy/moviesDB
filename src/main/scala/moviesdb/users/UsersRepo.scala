package moviesdb.users.sqlite

import cats.effect.IO
import cats.effect.kernel.MonadCancelThrow
import doobie.implicits.*
import doobie.util.query.Query0
import doobie.{ConnectionIO, Read, Transactor}
import moviesdb.domain.{PasswordHash, User, UserName}
import moviesdb.users.UsersRepoAlgebra

class UsersRepo[F[_]: MonadCancelThrow](xa: Transactor[F]) extends UsersRepoAlgebra[F]:
  import UsersRepo.*
  def getUser(userName: UserName, passwordHash: PasswordHash): F[Option[User]] =
    getUserQry(userName, passwordHash)
      .option
      .transact(xa)

object UsersRepo:
  // private so the query are not visible outside
  // the object remains public, so the apply method can be used
  private[sqlite] def getUserQry(userName: UserName, passwordHash: PasswordHash): Query0[User] =
    sql"SELECT id, name FROM users WHERE name = $userName AND password_hash = $passwordHash"
      .query[User]
