package moviesdb.users

import moviesdb.domain.*

trait UsersRepoAlgebra[F[_]]:
  def getUser(userName: UserName, passwordHash: PasswordHash): F[Option[User]]
