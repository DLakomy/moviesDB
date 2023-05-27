package moviesdb.domain

import io.circe.derivation.Configuration as CirceConfiguration
import io.circe.{Decoder as CDecoder, Encoder as CEncoder}
import sttp.tapir.*
import sttp.tapir.Schema.annotations.*
import sttp.tapir.generic.auto.*
import sttp.tapir.generic.{Configuration, Derived}
import sttp.tapir.json.circe.*

import java.util.UUID

object Movies:
  trait Identifiable:
    def id: MovieId

  case class ProductionYear(year: Int) extends AnyVal
  // TBH no idea why not an opaque type; I guess some derivation issues
  case class MovieId(value: UUID) extends AnyVal

  // of course it should have a variant with ID, but I want to simplify the exercise
  case class Episode(title: String, year: ProductionYear, number: Int)

  sealed trait NewMovie
  case class NewStandalone(title: String, year: ProductionYear) extends NewMovie
  case class NewSeries(title: String, episodes: List[Episode]) extends NewMovie

  sealed trait Movie extends Identifiable
  case class Standalone(id: MovieId, title: String, year: ProductionYear) extends Movie
  case class Series(id: MovieId, title: String, episodes: List[Episode]) extends Movie

  private val discriminatorFieldName = "objectType"
  // circe start
  given CirceConfiguration = CirceConfiguration.default.withDiscriminator(discriminatorFieldName)
  given CEncoder[ProductionYear] = CEncoder.encodeInt.contramap(_.year)
  given CDecoder[ProductionYear] = CDecoder.decodeInt.map(ProductionYear.apply)
  given CEncoder[MovieId] = CEncoder.encodeString.contramap(_.value.toString)
  given CDecoder[MovieId] = CDecoder.decodeString.map(s => MovieId(UUID.fromString(s)))
  given CDecoder[Episode] = CDecoder.derivedConfigured
  given CEncoder[Episode] = CEncoder.AsObject.derivedConfigured
  given CEncoder[NewMovie] = CEncoder.AsObject.derivedConfigured
  given CDecoder[NewMovie] = CDecoder.derivedConfigured
  given CEncoder[Movie] = CEncoder.AsObject.derivedConfigured
  given CDecoder[Movie] = CDecoder.derivedConfigured
  // circe stop

  // tapir start
  given Configuration =
    Configuration.default.withDiscriminator(discriminatorFieldName)
  given Schema[ProductionYear] =
    Schema(SchemaType.SInteger()).validate(Validator.min(1800).contramap(_.year))
  given Schema[MovieId] = Schema(SchemaType.SString()).format("UUID")
  given Schema[Episode] =
    summon[Derived[Schema[Episode]]].value.modify(_.number)(_.validate(Validator.min(1)))
  given Schema[NewMovie] =
    summon[Derived[Schema[NewMovie]]]
      .value.modifyUnsafe(discriminatorFieldName)(_.copy(description = Some("discriminator")))
  given Schema[Movie] =
    summon[Derived[Schema[Movie]]]
      .value.modifyUnsafe(discriminatorFieldName)(_.copy(description = Some("discriminator")))
  // tapir stop
