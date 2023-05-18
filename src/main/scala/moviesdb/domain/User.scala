package moviesdb.domain

import doobie.{Get, Put}

opaque type UserId = Int
object UserId:
  def apply(id: Int): UserId = id
  given (using g: Get[Int]): Get[UserId] = g

opaque type PasswordHash = String
object PasswordHash:
  def apply(hash: String): PasswordHash = hash
  given (using g: Put[String]): Put[PasswordHash] = g

opaque type UserName = String
object UserName:
  def apply(userName: String): UserName = userName
  given (using g: Get[String]): Get[UserName] = g
  given (using g: Put[String]): Put[UserName] = g

final case class User(id: UserId, name: UserName)
