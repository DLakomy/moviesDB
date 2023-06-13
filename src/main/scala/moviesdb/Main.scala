package moviesdb

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Env
import com.comcast.ip4s.{Host, Hostname, Port, port}
import doobie.ExecutionContexts
import doobie.util.transactor.Transactor
import moviesdb.core.HashingAlgs
import moviesdb.movies.sqlite.MoviesRepo
import moviesdb.movies.{MoviesRepoAlgebra, MoviesService}
import moviesdb.sqliteSupport.Utils.{dataSourceFromConnString, dataSourceFromPath, inMemoryConnString}
import moviesdb.users.UsersService
import moviesdb.users.sqlite.UsersRepo
import org.flywaydb.core.Flyway
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.sqlite.SQLiteDataSource
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.nio.file.{Path, Paths}

object Main extends IOApp:

  case class ServerConfig(host: Host, port: Port)
  case class DatabaseConfig(dbPath: Path, poolSize: Int)
  case class Config(serverCfg: ServerConfig, dbCfg: DatabaseConfig)

  // in a full fledged application these param would go to a separate file
  private def program(cfg: Config): IO[ExitCode] =

    val ds = dataSourceFromPath(cfg.dbCfg.dbPath)
    val transactor =
      for ce <- ExecutionContexts.fixedThreadPool[IO](cfg.dbCfg.poolSize)
      yield Transactor.fromDataSource[IO](ds, ce)

    def runServer(routes: HttpRoutes[IO]) =
      EmberServerBuilder
        .default[IO]
        .withHost(cfg.serverCfg.host)
        .withPort(cfg.serverCfg.port)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build
        .use { server =>
          IO.println(s"Go to http://${cfg.serverCfg.host}:${server.address.getPort}/docs to open SwaggerUI.") >>
          IO.never
        }

    val initDb = IO {
      val fw: Flyway =
        Flyway
          .configure()
          .dataSource(ds)
          .locations("classpath:db/migration")
          .load()

      fw.migrate()
    }

    transactor.use { xa =>
      for
        _ <- initDb
        usersRepo  = UsersRepo[IO](xa)
        usersSrv   = UsersService[IO](usersRepo, HashingAlgs.sha256)
        moviesRepo = MoviesRepo[IO](xa)
        moviesSrv  = MoviesService(moviesRepo)
        endpoints = MovieEndpoints(moviesSrv, usersSrv).all
        routes = Http4sServerInterpreter[IO]().toRoutes(endpoints)
        _ <- runServer(routes)
      yield ExitCode.Success
    }
  end program


  override def run(args: List[String]): IO[ExitCode] = for
    maybeHost       <- Env[IO].get("MOVIESDB_HOST")
    maybePort       <- Env[IO].get("MOVIESDB_PORT")
    maybeDbPath     <- Env[IO].get("MOVIESDB_DBPATH")
    maybeDbPoolSize <- Env[IO].get("MOVIESDB_DBPOOLSIZE")

    // in case of parse failure I use the default (not ideal of course, should report an error)
    // easy to achieve by using a config parsing library, eg. lightbend/config
    host       = maybeHost.flatMap(Host.fromString).getOrElse(Host.fromString("localhost").get)
    port       = maybePort.flatMap(_.toIntOption.flatMap(Port.fromInt)).getOrElse(port"8080")
    dbPath     = maybeDbPath.map(Paths.get(_)).getOrElse(Paths.get("movies.db"))
    dbPoolsize = maybeDbPoolSize.flatMap(_.toIntOption).getOrElse(32)

    serverConfig = ServerConfig(host, port)
    databaseConfig = DatabaseConfig(dbPath, dbPoolsize)
    config = Config(serverConfig, databaseConfig)

    exitCode <- program(config)
  yield exitCode


