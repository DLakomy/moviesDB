package moviesdb

import moviesdb.Endpoints.{*, given}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.server.stub.TapirStubInterpreter

import Movies.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.generic.auto.*
import sttp.client3.circe.*
import sttp.tapir.integ.cats.CatsMonadError

class EndpointsSpec extends AnyFlatSpec with Matchers with EitherValues:

  it should "list available movies" in {
    // given
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpoint(moviesListingServerEndpoint)
      .thenRunLogic()
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://test.com/movies")
      .response(asJson[List[NewMovie]])
      .send(backendStub)

    // then
    response.map(_.body.value shouldBe exampleMovies).unwrap
  }

  extension [T](t: IO[T]) def unwrap: T = t.unsafeRunSync()
