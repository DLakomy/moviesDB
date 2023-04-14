package moviesdb.domain

opaque type UserId = Int
final case class User(id: UserId, name: String)
