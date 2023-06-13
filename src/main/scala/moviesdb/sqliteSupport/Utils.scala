package moviesdb.sqliteSupport

import cats.effect.Sync
import org.flywaydb.core.Flyway
import org.sqlite.SQLiteDataSource

import java.nio.file.Path

object Utils:
  def connStringForPath(path: String): String = s"jdbc:sqlite:$path?foreign_keys=on;"

  def inMemoryConnString(dbName: String): String = s"jdbc:sqlite:file:$dbName?mode=memory&cache=shared"

  def dataSourceFromConnString(connString: String): SQLiteDataSource =
    val sqliteDb = new SQLiteDataSource()
    sqliteDb.setUrl(connString)
    sqliteDb.setEnforceForeignKeys(true)
    sqliteDb

  def dataSourceFromPath(path: Path): SQLiteDataSource =
    dataSourceFromConnString(s"jdbc:sqlite:$path?foreign_keys=on;")

  def initDb[F[_]: Sync](dataSource: SQLiteDataSource): F[Unit] = Sync[F].blocking {
    val fw: Flyway =
      Flyway
        .configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()

    fw.migrate()
    ()
  }
