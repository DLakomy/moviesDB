package moviesdb

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port, port}
import moviesdb.movies.{MoviesRepo, MoviesRepoAlgebra, MoviesService}
import org.flywaydb.core.Flyway
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.sqlite.SQLiteDataSource
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =

    // TODO placeholder, so it compiles (it's not meant to work yet)
    val repo = MoviesRepo[IO]

    val routes = Http4sServerInterpreter[IO]().toRoutes(Endpoints(MoviesService[IO](repo)).all)

    val port = sys.env
      .get("HTTP_PORT")
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(port"8080")

    // TODO move to some other file
    val runServer =
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

    // TODO use Utils
    val initDb = IO{
      val sqliteDb = new SQLiteDataSource()
      // TODO parametrize db location
      sqliteDb.setUrl("jdbc:sqlite:movies.db?foreign_keys=on;")

      val fw: Flyway =
        Flyway
          .configure()
          .dataSource(sqliteDb)
          .locations("classpath:db/migration")
          .load()

      fw.migrate()
    }

    initDb >> runServer
