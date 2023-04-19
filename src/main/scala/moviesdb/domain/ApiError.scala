package moviesdb.domain

sealed trait ErrorInfo:
  def info: String

object ApiError:
  case object IdMismatch extends ErrorInfo:
    val info = "Id in the path is different than this in the payload"

  case object MovieNotFound extends ErrorInfo:
    val info = "Movie not found"

  case object Unauthorized extends ErrorInfo:
    val info = "You are not authorized"

  case class InvalidData(val info: String) extends ErrorInfo
