package moviesdb.users

import moviesdb.domain.*

trait UsersServiceAlgebra[F[_]]:
  def getUser(userName: UserName, password: String): F[Option[User]]
