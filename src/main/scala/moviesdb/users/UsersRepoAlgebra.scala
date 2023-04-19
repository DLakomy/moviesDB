package moviesdb.users

import moviesdb.domain.{ErrorInfo, PasswordHash, User, UserId}

trait UsersRepoAlgebra[F[_]]:
  def getUser(id: UserId, passwordHash: PasswordHash): F[Option[User]]
