package moviesdb.core

import moviesdb.domain.PasswordHash

import java.math.BigInteger
import java.security.MessageDigest

object HashingAlgs:
  def sha256(password: String): PasswordHash =
    val hash = String.format(
      "%032x",
      BigInteger(
        1,
        MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"))
      )
    )
    val hash64 = hash.reverse.padTo(64, "0").reverse.mkString
    PasswordHash(hash64)
