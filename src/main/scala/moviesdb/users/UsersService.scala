package moviesdb.users
import moviesdb.domain.*

import java.math.BigInteger
import java.security.MessageDigest

class UsersService[F[_]](usersRepo: UsersRepoAlgebra[F], hashingFn: String => PasswordHash) extends UsersServiceAlgebra[F]:

  override def getUser(userName: UserName, password: String): F[Option[User]] =
    usersRepo.getUser(userName, hashingFn(password))

