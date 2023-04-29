package moviesdb.domain

sealed trait DbErrorInfo:
  def info: String

object DbError:
  case object MovieNotFound extends DbErrorInfo:
    val info = "Movie not found"

  case class InvalidData(info: String) extends DbErrorInfo
