package moviesdb.sqliteSupport

import cats.effect.Sync
import org.flywaydb.core.Flyway
import org.sqlite.SQLiteDataSource

object Utils:
  def connStringForPath(path: String): String = s"jdbc:sqlite:$path?foreign_keys=on;"

  val inMemoryConnString: String = "jdbc:sqlite:file:memdb1?mode=memory&cache=shared"

  def dataSourceFromConnString(connString: String): SQLiteDataSource =
    val sqliteDb = new SQLiteDataSource()
    sqliteDb.setUrl(connString)
    sqliteDb.setEnforceForeignKeys(true)
    sqliteDb


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
