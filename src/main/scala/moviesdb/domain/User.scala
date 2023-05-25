package moviesdb.domain

import doobie.{Get, Put, Read, Write}

import java.util.UUID

opaque type UserId = UUID
object UserId:
  def apply(id: UUID): UserId = id
  given Read[UserId] = Read[String].map(UUID.fromString)
  given Write[UserId] = Write[String].contramap(_.toString)


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
