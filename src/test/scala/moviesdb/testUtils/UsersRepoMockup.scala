package moviesdb.testUtils

import cats.Applicative
import moviesdb.domain.*
import moviesdb.users.UsersRepoAlgebra
import cats.syntax.all.*

class UsersRepoMockup[F[_]: Applicative](whiteList: List[UsersRepoMockup.User]) extends UsersRepoAlgebra[F]:
  override def getUser(userName: UserName, passwordHash: PasswordHash): F[Option[User]] =
    whiteList
      .find(n => n.name == userName && n.passwordHash == passwordHash)
      .map(n => User(n.id, n.name))
      .pure[F]

object UsersRepoMockup:
  case class User(id: UserId, passwordHash: PasswordHash, name: UserName)
