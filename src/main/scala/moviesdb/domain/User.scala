package moviesdb.domain

opaque type UserId = Int
object UserId:
  def apply(id: Int): UserId = id

opaque type PasswordHash = String
object PasswordHash:
  def apply(hash: String): PasswordHash = hash

final case class User(id: UserId, name: String)
