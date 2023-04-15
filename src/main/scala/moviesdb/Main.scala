package moviesdb

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port, port}
import moviesdb.movies.MoviesServiceLive
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =

    val routes = Http4sServerInterpreter[IO]().toRoutes(Endpoints(MoviesServiceLive[IO]).all)

    val port = sys.env
      .get("HTTP_PORT")
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(port"8080")

    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(port)
      .withHttpApp(Router("/" -> routes).orNotFound)
      .build
      .use { server =>
        IO.println(s"Go to http://localhost:${server.address.getPort}/docs to open SwaggerUI.") >>
        IO.never
      }
      .as(ExitCode.Success)
