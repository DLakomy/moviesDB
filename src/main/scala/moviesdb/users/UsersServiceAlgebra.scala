package moviesdb.users

import moviesdb.domain.{ErrorInfo, PasswordHash, User, UserId}

trait UsersServiceAlgebra[F[_]]:
  def getUser(id: UserId, password: String): F[Option[User]]
  protected def hashPassword(password: String): PasswordHash
