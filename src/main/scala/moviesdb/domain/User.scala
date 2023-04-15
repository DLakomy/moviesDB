package moviesdb.domain

opaque type UserId = Int
object UserId:
  def apply(id: Int): UserId = id

final case class User(id: UserId, name: String)
