package moviesdb.domain

sealed trait ApiErrorInfo:
  def info: String

object ApiError:
  case object IdMismatch extends ApiErrorInfo:
    val info = "Id in the path is different than this in the payload"

  case object MovieNotFound extends ApiErrorInfo:
    val info = "Movie not found"

  case object Unauthorized extends ApiErrorInfo:
    val info = "You are not authorized"

  case class InvalidData(info: String) extends ApiErrorInfo
